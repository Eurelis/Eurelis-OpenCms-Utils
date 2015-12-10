package com.eurelis.opencms.utils.tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.flex.CmsFlexController;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.jsp.Messages;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.relations.CmsCategory;
import org.opencms.util.CmsStringUtil;

import com.eurelis.opencms.utils.tag.CmsJspTagLocalizeProperty;


/**
 * This tag is used to order a list of CmsResource or CmsCategory.<p>
 * 
 * It returns a map Map&lt;String,List&lt;CmsResource>> or Map&lt;String,List&lt;CmsCategory>>.<p>
 * 
 * <code>&lt;utils:orderListByLocalizedProperty var="${mapvar}" locale="${locale}" name="${propertyname}" list="${thelisttoorder}" /&gt;</code><p>
 * 
 * Use as :<br/>
 * <pre>
 *  &lt;%	
	org.opencms.relations.CmsCategoryService cs = org.opencms.relations.CmsCategoryService.getInstance();
	java.util.List&lt;org.opencms.relations.CmsCategory> categoriesCustomerTypo = cs.readCategories(cmsAction.getCmsObject(), "customer-typology/", false, cmsAction.getCmsObject().getRequestContext().getSiteRoot());
	%>
	&lt;c:set var="listCustomerTypo" value="&lt;%=categoriesCustomerTypo %&gt;" /&gt;
	&lt;utils:orderListByLocalizedProperty var="mapCustomerTypo" locale="${locale}" name="Title" list="${listCustomerTypo}" /&gt;
 * </pre>
 * 
 * @author Sandrine Prousteau
 * @version 0.0.1
 * @since com.eurelis.opencms.utils 1.0.1.3
 *
 */
public class CmsJspTagOrderListByLocalizedProperty extends TagSupport{

	private static final long serialVersionUID = 1L;
	
	/** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspTagOrderListByLocalizedProperty.class);
    
    /** The locale. */
    private String m_locale;
    
    /** The name of the property to read. */
    private String m_propertyName;
    
    /** The list. */
    private List m_list;
    
    /** The name of the variable to store the value. */
    private String m_varName;
    
    
    /** The name of the property extracted. */
    public static String DEFAULTLOCALE = "en";
    
    protected CmsObject cmsObject;
    
    
    /**
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {

        release();
        return EVAL_PAGE;
    }
    
    /**
     * @return SKIP_BODY
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
    	
    	if(LOG.isDebugEnabled()) LOG.debug("orderListByTitle...");
    	
    	ServletRequest req = pageContext.getRequest();
    	HttpSession session = pageContext.getSession();

        // This will always be true if the page is called through OpenCms 
        if (CmsFlexController.isCmsRequest(req)) {
        	
        	cmsObject = CmsFlexController.getCmsObject(req);

            try {
            	
            	List mylist = getList();
            	String locale = getLocale();
                String name = getName();
            	
    			Map<String,List> result = new TreeMap<String,List>();
                
    			if(mylist!=null && !mylist.isEmpty()){
    				Iterator it = mylist.iterator();
    				while(it.hasNext()){
    					Object obj = it.next();
    					
    					String title = "";
    					if (obj instanceof CmsResource){
    						String path = cmsObject.getRequestContext().removeSiteRoot(((CmsResource) obj).getRootPath());
    						String filename = ((CmsResource) obj).getName();
    						title = CmsJspTagLocalizeProperty.localizePropertyTagAction(name, path, filename, locale, req).toLowerCase();
    						LOG.debug("  " + ((CmsResource) obj).getRootPath() + " => " + title);
    					}else if (obj instanceof CmsCategory){
    						String path = cmsObject.getRequestContext().removeSiteRoot(((CmsCategory) obj).getRootPath());
    						String filename = ((CmsCategory) obj).getName();
    						title = CmsJspTagLocalizeProperty.localizePropertyTagAction(name, path, filename, locale, req).toLowerCase();
    						LOG.debug("  " + ((CmsCategory) obj).getRootPath() + " => " + title);
    					}else{
    						title = obj.toString();
    						LOG.debug("  " + " => " + title);
    					}
    					
    					List keyList = new ArrayList();
    					if(result.containsKey(title)){
    						keyList = result.get(title);
    						if(keyList.contains(obj)){
    							keyList.add(obj);
    						}
    					}else{
    						keyList.add(obj);
    					}
    					result.put(title, keyList);
    					
    					
    					
    				}
    			}
    			
    			if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(getVar())){
                	pageContext.setAttribute(getVar(), result);
                }else{
                	pageContext.getOut().print(result);
                }
            	
            } catch (Exception ex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(Messages.get().getBundle().key(Messages.ERR_PROCESS_TAG_1, "orderResourcesByTitle"), ex);
                }
                throw new javax.servlet.jsp.JspException(ex);
            }
        }
        
    	return SKIP_BODY;
    }
    
    
    /**
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    @Override
    public void release() {

        super.release();
        m_locale = null;
        m_propertyName = null;
        m_list = null;
    }
    
    
    
    
    
    
    
    public List getList(){
    	return m_list;
    }
    
    public String getLocale() {
		String defaultLocale = DEFAULTLOCALE;
    	try {
    		CmsJspActionElement bean = new CmsJspActionElement(pageContext, (HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse());
    		String locale = bean.getCmsObject().readPropertyObject(bean.getCmsObject().getRequestContext().getUri(), CmsPropertyDefinition.PROPERTY_LOCALE, true).getValue(); 
			if(LOG.isDebugEnabled()) LOG.debug("getLocale : search ascendant ... uri = " + bean.getCmsObject().getRequestContext().getUri() + " , locale = " + locale);
			if(!CmsStringUtil.isEmptyOrWhitespaceOnly(locale)){
				defaultLocale = locale;
			}
		} catch (CmsException e) {
			e.printStackTrace();
			LOG.error(e);
		}
        return CmsStringUtil.isNotEmptyOrWhitespaceOnly(m_locale) ? m_locale : defaultLocale;
	}
    
    public String getName() {
		return m_propertyName;
	}
    
    public String getVar() {
		return m_varName;
	}
    
    public void setList(List list) {
		this.m_list = list;
	}
    
    public void setLocale(String locale) {
		this.m_locale = locale;
	}
    
    public void setName(String propertyName) {
		this.m_propertyName = propertyName;
	}
    
    public void setVar(String varName) {
		this.m_varName = varName;
	}

	
}
