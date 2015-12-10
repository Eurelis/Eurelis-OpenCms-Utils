<%@page buffer="none" session="true" taglibs="c,cms,fn,fmt" language="java" contentType="text/xml; charset=UTF-8" pageEncoding="UTF-8" %>
<%@page import="java.sql.*,java.lang.*,java.util.*,java.util.Map.Entry,java.util.regex.Pattern"%>
<%@page import="org.dom4j.Document,org.dom4j.DocumentHelper,org.dom4j.Element"%>
<%@page import="javax.servlet.http.HttpServletResponse"%>
<%@page import="org.opencms.configuration.*,org.opencms.db.CmsSqlManager,org.opencms.main.*,org.opencms.jsp.*"%>

<jsp:useBean id="actionbean" class="org.opencms.jsp.CmsJspActionElement">
<% actionbean.init(pageContext, request, response); %>
</jsp:useBean>
<%


  /*
  * Read help Parameter 
  *    -> if true get HTML Help Page
  */
  boolean helpRequired = false;
  String helpParam = request.getParameter("help");
  if(helpParam  != null){
    try{ 
      helpRequired = Boolean.parseBoolean(helpParam);   
    }catch(NumberFormatException e){}     
  }
  

  //set the content type as xml
  if(!helpRequired)
    actionbean.setContentType("text/xml");

  
  /*
   * ************************
   * Variable declarations 
   *   -> Theses global variables will be during result display   
   * ************************
   */

  String valueAttribut = "KO"; 
  boolean anError = false;
  String error = null;
  long timeBeforeRequest =0;
  long timeAfterRequest=0;
  Map configParameter = null;
  CmsConfigurationManager myconfig = null;
  
  List poolsName = new ArrayList();
  int nbrActivesConnections = 0;
  int nbrIdlesConnections = 0;
  CmsSqlManager sqlManager = null;
  
  Map poolStrategy = new HashMap();
  Map maxActivesConfigurated = new HashMap();
  Map responseTime = new HashMap();
  Map dbStatus = new HashMap();
  Map poolURLs = new HashMap();
  Map alerts = new HashMap();
  Map activesConnectionPerPool = new HashMap();
  
  /*
   * *******************************
   * The default value of parameters
   * *******************************
   */
  boolean use_500 = false;
  boolean displayConfig = false;
  boolean displayAlert = true;
  int alertLevel = 80;
  
  try{
    /* 
     * ***************
     * Read parameters 
     * ***************
     */
    String use500Error = request.getParameter("forceErrorOnAlert");     
    if(use500Error  != null){
      try{ 
        use_500 = Boolean.parseBoolean(use500Error);  
      }catch(NumberFormatException e){}     
    }
    
    String displayConfigString = request.getParameter("displayConfig");     
    if(displayConfigString  != null){
      try{ 
        displayConfig = Boolean.parseBoolean(displayConfigString);  
      }catch(NumberFormatException e){}     
    }
    
    String displayAlertString = request.getParameter("displayAlert");     
    if(displayAlertString  != null){
      try{ 
        displayAlert = Boolean.parseBoolean(displayAlertString);  
      }catch(NumberFormatException e){}     
    }
    
    String alertLevelString = request.getParameter("alertLevel");     
    if(alertLevelString  != null){
      try{ 
        alertLevel = Integer.parseInt(alertLevelString);  
      }catch(NumberFormatException e){}     
    }
    
    
    
    /* 
     * **********************
     * Get the OpenCms config 
     * **********************
     *
     * Updated for OpenCms 8.0.2
     *
     */
    
      
    /* BEFORE */
    /*
    myconfig = new CmsConfigurationManager(OpenCms.getSystemInfo().getAbsoluteRfsPathRelativeToWebInf(CmsSystemInfo.FOLDER_CONFIG));
    
    ExtendedProperties configuration = null;
        try {
            configuration = CmsPropertyUtils.loadProperties(OpenCms.getSystemInfo().getConfigurationFileRfsPath());
            myconfig.setConfiguration(configuration);       
        
        } catch (Exception e) {
          anError = true;
      error = e.getMessage();     
        }
    
        configParameter = myconfig.getConfiguration();
        */
        
        /* AFTER */
        myconfig = new CmsConfigurationManager(OpenCms.getSystemInfo().getAbsoluteRfsPathRelativeToWebInf(CmsSystemInfo.FOLDER_CONFIG_DEFAULT));
    
        CmsParameterConfiguration propertyConfiguration = null;
        try {
            //configuration = CmsPropertyUtils.loadProperties(OpenCms.getSystemInfo().getConfigurationFileRfsPath());
            propertyConfiguration = new CmsParameterConfiguration(OpenCms.getSystemInfo().getConfigurationFileRfsPath());
            myconfig.setConfiguration(propertyConfiguration);       
        
        } catch (Exception e) {
          anError = true;
      error = e.getMessage();     
        }
    
        configParameter = myconfig.getConfiguration();
        
        /* 
     * *********************
     * Get the list of pools
     * *********************
     */
     
    String poolsNameProperty = (String) configParameter.get("db.pools");
        String[] poolNames = poolsNameProperty.split(",");
        for(int i =0; i< poolNames.length; i++){
          if(poolNames[i] != null){
            poolsName.add(poolNames[i]);
          }
        }
        
        //Get the Connection Manager
      sqlManager = OpenCms.getSqlManager();   
        
        /*
         * *********************************
         * Get the list of strategy per pool
         * *********************************
         */
         
        Iterator poolIterator =  poolsName.iterator();
        while(poolIterator.hasNext()){
          
          String poolName = (String)poolIterator.next();
          if(poolName != null){
            
            /* Get poolURL */
            Iterator poolsURLIterator = sqlManager.getDbPoolUrls().iterator();
            while(poolsURLIterator.hasNext()){
              String poolURL = (String) poolsURLIterator.next();
              if(poolURL != null && poolURL.endsWith(poolName)){
                poolURLs.put(poolName,poolURL);
              }
              
            }
            
            /* Get the pool strategy */
            String poolStrategyProperty = (String) configParameter.get("db.pool."+poolName+".whenExhaustedAction");
            if(poolStrategyProperty != null){
              poolStrategy.put(poolName,poolStrategyProperty);            
            }
            
            /* Get the pool max active */
             String maxActivesConfiguratedString = (String)configParameter.get("db.pool."+poolName+".maxActive");
               if(maxActivesConfiguratedString != null){
                try{ 
                  maxActivesConfigurated.put(poolName,Integer.parseInt(maxActivesConfiguratedString));  
              }catch(NumberFormatException e){}               
               }
            
            /* Get the DB response time and status */
               Connection conn = null;
               valueAttribut="KO";
          try{
            conn = sqlManager.getConnection(poolName);
            
            /*Get list of tables*/
            String showTableQuery = "select * from CMS_PROJECTS;";
            PreparedStatement stmt = null;
            stmt = conn.prepareStatement(showTableQuery);
            ResultSet result = null;
            try{
              timeBeforeRequest = System.currentTimeMillis();
              result = stmt.executeQuery(); 
              timeAfterRequest = System.currentTimeMillis();
            }catch(SQLException e){ 
              anError = true;
              error = e.getMessage();
            }
                  
            if(result!= null)
                valueAttribut="OK";
            
          }catch(Exception e){
            anError = true;
            error = e.getMessage();     
          }finally{
            conn.close();
          }
          
          responseTime.put(poolName,String.valueOf(timeAfterRequest-timeBeforeRequest));
          dbStatus.put(poolName,valueAttribut);
          
          
          
          
          }
        }
        
         
       
    
    
    
    
    
  }catch(Exception e){
    anError = true;
    error = e.getMessage(); 
  }
  
  
  if(!helpRequired){
%>
<report>
  <memory>
    <free-memory value="<%= Runtime.getRuntime().freeMemory() %>" comment="The amount of free memory in the Java Virtual Machine."/>
    <total-memory value="<%= Runtime.getRuntime().totalMemory() %>" comment="The total amount of memory in the Java virtual machine."/>
    <max-memory value="<%= Runtime.getRuntime().maxMemory() %>" comment="The maximum amount of memory that the Java virtual machine will attempt to use."/>
  </memory>
<% if(poolURLs != null && !poolURLs.isEmpty() && sqlManager != null){ %>
  <pools>
    <% 
    Iterator poolIterator = poolsName.iterator();
    while(poolIterator.hasNext()){
      String poolName = (String) poolIterator.next();
      String dbURL = (String) poolURLs.get(poolName);
    %>
    <pool name="<%= poolName %>" url="<%= dbURL %>">
      <poolStrategy value="<%= poolStrategy.get(poolName) %>" comment="The current pool stategy"/>
      <maxPoolSize value="<%= maxActivesConfigurated.get(poolName) %>" comment="The max of actives connection configured"/>
          
    <%  
      if(dbURL != null){
        int actives = sqlManager.getActiveConnections(dbURL); 
        activesConnectionPerPool.put(poolName, new Integer(actives));
        int idles = sqlManager.getIdleConnections(dbURL); 
        float pourcentage = (actives * 100f)/(1f* (Integer)maxActivesConfigurated.get(poolName));
        
        if(pourcentage >= alertLevel){
          alerts.put(poolName,new Float(pourcentage));          
        }
        
        nbrActivesConnections += actives;
        nbrIdlesConnections+=idles;
    %>
      <activeconnections value="<%= actives %>" comment="The number of actives connection for the pool <%= poolName %>"/>
      <idleconnections value="<%= idles %>" comment="The number of idle connection for the pool <%= poolName %>"/>
      <currentUsagePercentage value="<%= pourcentage %>" comment="The current pourcentage usage of the DB pool (relative to the max number connections configured)"/>
      
      <%}%>
    </pool>
    <% } %> 
  </pools>
<%}%>

<% if(displayConfig && configParameter !=null && !configParameter.isEmpty()){ %>
  <configuration>
  <%
    Iterator configIterator = (new TreeMap(configParameter)).entrySet().iterator();
    while(configIterator.hasNext()){
      Entry entry = (Entry)configIterator.next();
      if(entry != null){
        String key = (String)entry.getKey();
        if(!Pattern.matches("^db\\.pool\\.[a-zA-Z0-9]+\\.jdbcUrl$",key)
          && !Pattern.matches("^db\\.pool\\.[a-zA-Z0-9]+\\.user$",key) 
          && !Pattern.matches("^db\\.pool\\.[a-zA-Z0-9]+\\.password$",key)){
  %>
    <configelement name="<%= entry.getKey() %>" value="<%= entry.getValue()%>"/>
  <%    
        }
      }
    }
  %>
  
  </configuration>
<% } %>

<% if(displayAlert){ 
  
    if(!alerts.isEmpty()){
    
%>
  <alerts>
    <%
    Iterator alertIterator = alerts.entrySet().iterator();
    while(alertIterator.hasNext()){
      Entry alertEntry =(Entry)alertIterator.next();
      if(alertEntry != null){
        String poolNameAlert = (String)alertEntry.getKey();
        if(poolNameAlert != null){
          %>      
    <alert pool="<%= poolNameAlert %>" message="The DB Pool utilisation is height for pool <%= poolNameAlert %> !" strategy="<%= poolStrategy.get(poolNameAlert) %>" maxConnectionConfigurated="<%= maxActivesConfigurated.get(poolNameAlert) %>" currentUsage="<%= activesConnectionPerPool.get(poolNameAlert) %>" usagePourcentage="<%= alerts.get(poolNameAlert) %>"/>
          <%
        }       
      } 
    }
    %>
  </alerts>
  <%
      if(use_500){
        actionbean.setStatus(500);  
      }
    }
  } %>

<% if(anError){ %>
  <error message="<%= error.replaceAll("\"","") %>"/> 
<%} %>

  
</report>
<%
  if(anError){
    actionbean.setStatus(500);    
  } 
%>
<%} else{ //not help required -> HTML page with parameters descriptions %>

<html>
  <head>
    <title>Monitoring user guide</title>
    <STYLE type="text/css">
    <!--

      .name{
        font-weight:bold;
      }

      .default{
        font-style:italic;
      }

      .description{
        text-align: left;
      }

      table th, table td{
        text-align: center;
      }

      table th{
        background-color:#ECECEC;
      }

      table{
        border: 1px solid #000000;
      }

      

    -->
    </STYLE>
  </head>
  <body>
    <h1>Monitoring user guide</h1>
    <p>This jsp page allows to get some informations about configurations and DB usage.</p>
    <p>Some parameters can be used. The following table will display theses :</p>
    <table>
      <tr>  
        <th>Option name</th>
        <th>Required ?</th>
        <th>Default value</th>
        <th>Description</th>
      </tr>
      <tr>
        <td class="name">help</td>
        <td>No</td>
        <td class="default">false</td>
        <td class="description">Display the help page of the sonde (the current one)</td>
      </tr>
      <tr>
        <td class="name">forceErrorOnAlert</td>
        <td>No</td>
        <td class="default">false</td>
        <td class="description">Return a 500 Error if an alert of DB over-usage occurs</td>
      </tr>
      <tr>
        <td class="name">displayConfig</td>
        <td>No</td>
        <td class="default">false</td>
        <td class="description">Display the all configuration of the DB (except DB name, DB user name, DB user password) &rarr; the content of opencms.properties</td>
      </tr>
      <tr>
        <td class="name">displayAlert</td>
        <td>No</td>
        <td class="default">true</td>
        <td class="description">Display alert if the DB pool usage is over the alertLevel</td>
      </tr>
      <tr>
        <td class="name">alertLevel</td>
        <td>No</td>
        <td class="default">80</td>
        <td class="description">The pourcentage of DB pool usage that will throw a alert if over (<b>Must be an Integer </b>)</td>
      </tr>
    </table>
  </body>
</html>

<%}%>
