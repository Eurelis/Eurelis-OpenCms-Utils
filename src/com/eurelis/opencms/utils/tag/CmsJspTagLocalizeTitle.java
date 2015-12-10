/**
 * 
 */
package com.eurelis.opencms.utils.tag;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.logging.Log;
import org.opencms.file.CmsProperty;
import org.opencms.flex.CmsFlexController;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.jsp.Messages;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.staticexport.CmsLinkManager;
import org.opencms.util.CmsStringUtil;
import javax.servlet.jsp.JspException;


/**
 * This tag is used to localize Description property of a file, often a category folder.<p>
 * 
 * <code>&lt;utils:localizeTitle file="${mycategory}" locale="${locale}" default="${categoryname}" /&gt;</code><p>
 * 
 * The Description property must be edited like this : <code>[locale]:[localized value]|[locale]:[localized value]</code>. 
 * For example : fr:Mon titre|en:My title|de:Mein titel<p> 
 * 
 * If no localized label is found in Description property, the Title property is returned. If Title property is empty, the default value
 * is returned. If there is no default value, an empty value is returned.
 * 
 * Attribute <code>locale</code> is not needed. If it's not set, the property "locale" is searched in the ascendance of the URI. 
 * If it's still not set, the default locale "en" is used.
 * 
 * To use a more configurable tag, see CmsJspTagLocalizeProperty tag.
 * 
 * 
 * @author Sandrine Prousteau
 * @version 0.0.1
 * @since com.eurelis.opencms.utils 1.0.0.0
 *
 */
public class CmsJspTagLocalizeTitle extends TagSupport{
	
	/** Constants for <code>file</code> attribute interpretation. */
    private enum FileUse {

        /** Use element uri. */
        ELEMENT_URI("element.uri"),
        /** Use parent (same as {@link #URI}). */
        PARENT("parent"),
        /** Use search (same as {@link #SEARCH_URI}). */
        SEARCH("search"),
        /** Use search element uri. */
        SEARCH_ELEMENT_URI("search.element.uri"),
        /** Use search parent (same as {@link #SEARCH_URI}). */
        SEARCH_PARENT("search-parent"),
        /** Use seach this (same as {@link #SEARCH_ELEMENT_URI}). */
        SEARCH_THIS("search-this"),
        /** Use search uri. */
        SEARCH_URI("search.uri"),
        /** Use sitemap entries. */
        /** Use this (same as {@link #ELEMENT_URI}). */
        THIS("this"),
        /** Use uri. */
        URI("uri");

        /** Property name. */
        private String m_name;

        /** Constructor.<p>
         * @param name the string representation of the constant  
         **/
        private FileUse(String name) {

            m_name = name;
        }

        /**
         * Parses a string into an enumeration element.<p>
         * 
         * @param name the name of the element
         * 
         * @return the element with the given name or <code>null</code> if not found
         */
        public static FileUse parse(String name) {

            for (FileUse fileUse : FileUse.values()) {
                if (fileUse.getName().equals(name)) {
                    return fileUse;
                }
            }
            return null;
        }

        /** 
         * Returns the name.<p>
         * 
         * @return the name
         */
        public String getName() {

            return m_name;
        }
    }
	
	/** Serial version UID required for safe serialization. */
	private static final long serialVersionUID = 2015912860698450095L;

	/** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspTagLocalizeTitle.class);
    
    /** The default value. */
    private String m_defaultValue;
    
    /** The locale value. */
    private String m_localeValue;
    
    /** The file to read the property from. */
    private String m_propertyFile;
    
    /** The name of the property extracted. */
    public static String PROPERTYNAME = "Description";
    
    /** The name of the default property extracted. */
    public static String DEFAULTPROPERTYNAME = "Title";
    

    /**
     * 
     * Schema : <code>fr:Mon titre|en:My title|de:Mein titel</code>
     * 
     * @param value value containing localized labels. Should be like <code>fr:Mon titre|en:My title|de:Mein titel</code>.
     * @param locale locale code used to extract the label.
     * @return the label localized if found, of empty value if not found.
     * @throws CmsException
     */
    public static String extractLocalizedValue(String value, String locale) throws CmsException{
    	
    	String localizedValue = null;
    	
    	//if(LOG.isDebugEnabled()) LOG.debug("Localizing Value '"+value+"' with locale '"+locale+"'...");
    	if(CmsStringUtil.isEmptyOrWhitespaceOnly(value)){
    		localizedValue = "";
    		if(LOG.isDebugEnabled()) LOG.debug("Localizing Value : value is empty or whitespace only");
    	}else{
    		Map<String, String> map = CmsStringUtil.splitAsMap(value, "|", ":");
			if(map == null){
				localizedValue = "";
				if(LOG.isDebugEnabled()) LOG.debug("Localizing Value : value map is null");
			}else{
				if(map.isEmpty()){
					localizedValue = "";
					if(LOG.isDebugEnabled()) LOG.debug("Localizing Value : value map is empty");
				}else{
					if(!map.containsKey(locale)){
						localizedValue = "";
						if(LOG.isDebugEnabled()) LOG.debug("Localizing Value : value map doesn't contains key '"+locale+"'");
						//prend la 1ere traduction trouvee
						Set<String> set = map.keySet();
						if(set!=null){
							Iterator<String> it = set.iterator();
							String firstKey = (String) it.next();
							localizedValue = (String)map.get(firstKey);
						}
					}else{
						localizedValue = (String)map.get(locale);
						if(LOG.isDebugEnabled()) LOG.debug("Localizing Value : value map contains key '"+locale+"' and value is '"+localizedValue+"'");
					}
				}
			}
    	}
    	//if(LOG.isDebugEnabled()) LOG.debug("Localizing Value '"+value+"' with locale '"+locale+"' : '"+localizedValue+"'");
    	return localizedValue;
    }
    
    
    /**
     * Internal action method.<p>
     * 
     * @param action the search action
     * @param req the current request
     * 
     * @return Map the map of properties
     *      
     * @throws CmsException if something goes wrong
     */
    public static Map<String, String> propertiesTagAction(String action, ServletRequest req) throws CmsException {

        CmsFlexController controller = CmsFlexController.getController(req);

        FileUse useAction = FileUse.URI;
        if (action != null) {
            // if action is set overwrite default
            useAction = FileUse.parse(action);
        }

        String vfsUri = null;
        boolean search = false;
        if (useAction != null) {
            switch (useAction) {
                case URI:
                case PARENT:
                    // read properties of parent (i.e. top requested) file
                    vfsUri = controller.getCmsObject().getRequestContext().getUri();
                    break;
                case SEARCH:
                case SEARCH_URI:
                case SEARCH_PARENT:
                    // try to find property on parent file and all parent folders
                    vfsUri = controller.getCmsObject().getRequestContext().getUri();
                    search = true;
                    break;
                case ELEMENT_URI:
                case THIS:
                    // read properties of this file            
                    vfsUri = controller.getCurrentRequest().getElementUri();
                    break;
                case SEARCH_ELEMENT_URI:
                case SEARCH_THIS:
                    // try to find property on this file and all parent folders
                    vfsUri = controller.getCurrentRequest().getElementUri();
                    search = true;
                    break;
                default:
                    // just to prevent the warning since all cases are handled
            }
        } else {
            // read properties of the file named in the attribute  
            vfsUri = CmsLinkManager.getAbsoluteUri(action, controller.getCurrentRequest().getElementUri());
            search = false;
        }

        // now read the property from the VFS
        Map<String, String> value = new HashMap<String, String>();
        if (vfsUri != null) {
            value = CmsProperty.toMap(controller.getCmsObject().readPropertyObjects(vfsUri, search));
        }
        
        return value;
    }
    
    /**
     * Internal action method.<p>
     * 
     * @param property the property to look up
     * @param action the search action
     * @param defaultproperty the default property to look up
     * @param defaultValue the default value
     * @param locale the locale
     * @param req the current request
     * 
     * @return the localized value of the Description property, or the Title property is not found, or the default value if not found, or empty value if not found
     *      
     * @throws CmsException if something goes wrong
     */
    public static String localizeTitleTagAction(
        String property,
        String action,
        String defaultproperty,
        String defaultValue,
        String locale,
        ServletRequest req) throws CmsException {

        String value = propertiesTagAction(action, req).get(property);
        
        String defaultvalue = propertiesTagAction(action, req).get(defaultproperty);
        
        String localizedValue = extractLocalizedValue(value, locale);
        
        if (CmsStringUtil.isEmptyOrWhitespaceOnly(localizedValue)) {
        	localizedValue = defaultvalue;
        	if (CmsStringUtil.isEmptyOrWhitespaceOnly(localizedValue)) {
        		localizedValue = defaultValue;
        	}
        }
        return localizedValue;
    }
    
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
    	
    	//if(LOG.isDebugEnabled()) LOG.debug("doStartTag localizeTitle...");
    	
    	ServletRequest req = pageContext.getRequest();

        // This will always be true if the page is called through OpenCms 
        if (CmsFlexController.isCmsRequest(req)) {

            try {
                String prop = localizeTitleTagAction(PROPERTYNAME, getFile(), DEFAULTPROPERTYNAME, getDefault(), getLocale(), req);
                // Make sure that no null String is returned
                if (prop == null) {
                    prop = "";
                }
                //if(LOG.isDebugEnabled()) LOG.debug("doStartTag localizeTitle : "+prop);
                pageContext.getOut().print(prop);

            } catch (Exception ex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(Messages.get().getBundle().key(Messages.ERR_PROCESS_TAG_1, "localizeTitle"), ex);
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
        m_propertyFile = null;
        m_defaultValue = null;
        m_localeValue = null;
    }
    
    
    /**
     * Returns the default value.<p>
     * 
     * @return the default value
     */
    public String getDefault() {

        return m_defaultValue != null ? m_defaultValue : "";
    }
    
    /**
     * Returns the locale value.<p>
     * 
     * @return the locale value
     */
    public String getLocale() {
    	
    	String defaultLocale = "en";
    	try {
    		CmsJspActionElement bean = new CmsJspActionElement(pageContext, (HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse());
    		String locale = bean.getCmsObject().readPropertyObject(bean.getCmsObject().getRequestContext().getUri(), "locale", true).getValue(); 
			if(LOG.isDebugEnabled()) LOG.debug("getLocale : search ascendant ... uri = " + bean.getCmsObject().getRequestContext().getUri() + " , locale = " + locale);
			if(!CmsStringUtil.isEmptyOrWhitespaceOnly(locale)){
				defaultLocale = locale;
			}
		} catch (CmsException e) {
			e.printStackTrace();
		}

        return m_localeValue != null ? m_localeValue : defaultLocale;
    }
    
    /**
     * Returns the file name.<p>
     * 
     * @return the file name
     */
    public String getFile() {

        return m_propertyFile != null ? m_propertyFile : "parent";
    }
    
    /**
     * Sets the default value.<p>
     * 
     * This is used if a selected property is not found.<p>
     * 
     * @param def the default value
     */
    public void setDefault(String def) {

        if (def != null) {
            m_defaultValue = def;
        }
    }
    
    /**
     * Sets the locale value.<p>
     * 
     * This is used if a selected locale is not found.<p>
     * 
     * @param locale the locale value
     */
    public void setLocale(String locale) {

        if (locale != null) {
            m_localeValue = locale;
        }
    }
    
    /**
     * Sets the file name.<p>
     * 
     * @param file the file name
     */
    public void setFile(String file) {

        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(file)) {
            m_propertyFile = file;
        }
    }
    
    

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			System.out.println(extractLocalizedValue("fr:Mon titre|en:My title|de:Mein titel", "fr"));
			System.out.println(extractLocalizedValue("fr:Mon titre|en:My title|de:Mein titel", "it"));
			System.out.println(extractLocalizedValue("fr:Mon titre|en:My title|de:Mein titel", ""));
			System.out.println(extractLocalizedValue("fr:Mon titre|en:My title", "de"));
			System.out.println(extractLocalizedValue("fritre|en:My t:Mein titel", "fr"));
			System.out.println(extractLocalizedValue("fritre|en:My t:Mein titel", "en"));
			System.out.println(extractLocalizedValue("", "fr"));
			System.out.println(extractLocalizedValue("", "en"));
		} catch (CmsException e) {
			e.printStackTrace();
		}

	}

}
