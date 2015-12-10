package com.eurelis.opencms.utils.tag;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.flex.CmsFlexController;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.jsp.Messages;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.staticexport.CmsLinkManager;
import org.opencms.util.CmsStringUtil;




/**
 * This tag is used to get a label from a property of a file.<p>
 * 
 * <code>&lt;utils:localizeProperty file="${myfile}" name="${propertyname}" locale="${locale}" default="${filename}" /&gt;</code><p>
 * 
 * In order to get a localized value, the property must be edited like this : <code>[locale]=[localized value]|[locale]=[localized value]</code>. 
 * For example : fr=Mon titre|en=My title|de=Mein titel<p> 
 * NB : ":" has been replaced by "=" in order to allow ":" in labels.
 * 
 * If no localized label is found in the property, the property value is directly returned. If property is empty, the default value
 * is returned. If there is no default value, an empty value is returned.
 * 
 * If the property specified doesn't exists, an error is generated.
 * 
 * Attribute <code>locale</code> is not needed. If it's not set, the property "locale" is searched in the ascendance of the URI. 
 * If it's still not set, the default locale "en" is used.
 * 
 * If the attribute "var" is added, the value is returned is a context variable of this name. If not, the value is displayed.
 * 
 * 
 * @author Sandrine Prousteau
 * @version 0.0.2
 * @since com.eurelis.opencms.utils 1.0.1.0
 *
 */
public class CmsJspTagLocalizeProperty extends TagSupport{

	
	private static final long serialVersionUID = 1L;

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
    
    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspTagLocalizeProperty.class);
    
    /** The default value. */
    private String m_defaultValue;
    
    /** The locale. */
    private String m_locale;
    
    /** The file to read the property from. */
    private String m_file;
    
    /** The name of the variable to store the value. */
    private String m_varName;
    
    /** The name of the property to read. */
    private String m_propertyName;
    
    
    
    /** The name of the property extracted. */
    public static String DEFAULTLOCALE = "en";
    
    /** The name of the property extracted. */
    public static String SEPARATOR_LOCALES = "|";
    
    /** The name of the property extracted. */
    public static String SEPARATOR_VALUES = "=";

    /** The name of the session key for translations */
    public static final String TRANSLATIONS_KEY = "translations-properties";
    
    
    
    
    
    /**
     * 
     * Schema : <code>fr=Mon titre|en=My title|de=Mein titel</code>
     * 
     * @param value value containing localized labels. Should be like <code>fr=Mon titre|en=My title|de=Mein titel</code>.
     * @param locale locale code used to extract the label.
     * @return the label localized if found, of empty value if not found.
     * @throws CmsException
     */
    public static String extractLocalizedValue(String value, String locale) throws CmsException{
    	
    	String localizedValue = null;
    	if(isLocalizableFormatted(value)){
	    	Map<String,String> map = CmsStringUtil.splitAsMap(value, SEPARATOR_LOCALES, SEPARATOR_VALUES);
			if(map!=null && map.size()>=1){
				if(map.containsKey(locale)){
					localizedValue = map.get(locale);
					LOG.debug("extractLocalizedValue() value localized is " + localizedValue + "!!!" + " --- value = " + value + " locale = " + locale);
				}else if(map.containsKey(DEFAULTLOCALE)){
					localizedValue = map.get(DEFAULTLOCALE);
					LOG.debug("extractLocalizedValue() locale " + locale + " not found, get locale " + DEFAULTLOCALE + " instead!!! value localized is " + localizedValue + "!!!" + " --- value = " + value + " locale = " + locale);
				}else{
					Iterator<String> keys = map.keySet().iterator();
					String firstLocale = (String)keys.next();
					localizedValue = map.get(firstLocale);
					LOG.debug("extractLocalizedValue() locale " + locale + " and " + DEFAULTLOCALE + " not found, get locale " + firstLocale + " instead!!! value localized is " + localizedValue + "!!!" + " --- value = " + value + " locale = " + locale);
				}
			}else{
				LOG.warn("extractLocalizedValue() strange case!!! --- value = " + value + " locale = " + locale);
			}
    	}else{
    		LOG.debug("extractLocalizedValue() value is not well formatted for localization => return direct value");
    		localizedValue = value;
    	}
    	return localizedValue;
    }
    
    
    /**
     * Check if the value is localizable, ie contains at least a "=" separing 2 expressions.
     * If value is null or empty, return false;
     * 
     * @param value
     * @return
     * @throws CmsException
     */
    public static boolean isLocalizableFormatted(String value) throws CmsException{
    	
    	if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(value)){
    		return CmsStringUtil.validateRegex(value, ".*"+SEPARATOR_VALUES+".*", false); 
    	}
    	return false;
    }
    
    /**
     * Internal action method.<p>
     * 
     * @param propertyName the property name to look up
     * @param action the search action
     * @param defaultValue the default value
     * @param locale the locale
     * @param req the current request
     * 
     * @return the localized value of the property, or the default value if not found, or empty value if not found
     *      
     * @throws CmsException if something goes wrong
     */
    public static String localizePropertyTagAction(
        String propertyName,
        String action,
        String defaultValue,
        String locale,
        ServletRequest req) throws CmsException {

        String basicValue = propertiesTagAction(action, req).get(propertyName);
        
        String value = extractLocalizedValue(basicValue, locale);
        
        if(CmsStringUtil.isEmptyOrWhitespaceOnly(value)){
        	value = defaultValue;
        }
        
        return value;
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
        LOG.debug("   propertiesTagAction() : vfsUri = " + vfsUri);
        
        // Remove parameters and # (update version 0.0.3 -> 0.0.4)
        if(vfsUri==null || CmsStringUtil.isEmptyOrWhitespaceOnly(vfsUri)){
        	if (vfsUri == null) {
                LOG.warn("   propertiesTagAction() : vfsUri null");
        	}
        	if (CmsStringUtil.isEmptyOrWhitespaceOnly(vfsUri)) {
                LOG.warn("   propertiesTagAction() : vfsUri empty or white space only");
        	}
        }else{
        	if (vfsUri.contains("?")) {
                vfsUri = vfsUri.substring(0, vfsUri.indexOf("?"));
                LOG.debug("   The String value containing '?'. Substring is " + vfsUri + ".");
        	} else if (vfsUri.contains("#")) {
                vfsUri = vfsUri.substring(0, vfsUri.indexOf("#"));
                LOG.debug("   The String value containing '#'. Substring is " + vfsUri + ".");
        	}
        }

        // now read the property from the VFS
        Map<String, String> value = new HashMap<String, String>();
        if (vfsUri != null) {
            value = CmsProperty.toMap(controller.getCmsObject().readPropertyObjects(vfsUri, search));
        }
        
        return value;
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
    	
    	if(LOG.isDebugEnabled()) LOG.debug("localizeProperty...");
    	
    	ServletRequest req = pageContext.getRequest();
    	HttpSession session = pageContext.getSession();

        // This will always be true if the page is called through OpenCms 
        if (CmsFlexController.isCmsRequest(req)) {

            try {
            	
            	//Store and read values in session (update version 0.0.3 -> 0.0.4)
            	String prop = null;
                
                String file = getFile();
                String locale = getLocale();
                String name = getName();
                String defaultval = getDefault();
                LOG.debug("localizeProperty, file = " + file + "  locale = " + locale + " name = " + name + " default = " + defaultval);
            	
                
                if (!CmsFlexController.getCmsObject(req).getRequestContext().getCurrentProject().isOnlineProject()) {
                	LOG.debug("localizeProperty, is not Online");
                    prop = localizePropertyTagAction(name, file, defaultval, locale, req);
                }else{
                	LOG.debug("localizeProperty, is Online");
                	StringBuilder keyBldr = new StringBuilder();
                    String key = keyBldr.append(file).append("-").append(locale).append("-").append(name).toString();
                    LOG.debug("localizeProperty, key = " + key);
                    if(session==null){
                    	LOG.debug("localizeProperty, session is null, no translations");
                        prop = localizePropertyTagAction(name, file, defaultval, locale, req);
                    }else{
                  	  	Map<String, String> translations = (Map<String, String>) session.getAttribute(TRANSLATIONS_KEY);
                        if (translations == null) translations = new HashMap<String, String>();
                        if (translations.containsKey(key)) {
                        	prop = translations.get(key);
                        } else {
                        	prop = localizePropertyTagAction(name, file, defaultval, locale, req);
                        	if (prop == null) {
                        		prop = file;
                        	}
                        	LOG.debug("localizeProperty, add key " + key + " and prop " + prop + " in session "+TRANSLATIONS_KEY);
                        	translations.put(key, prop);
                        }
                        pageContext.getSession().setAttribute(TRANSLATIONS_KEY, translations);
                    }
                }
            	
                // Make sure that no null String is returned
                if (prop == null) {
                  prop = "";
                }
            	
                if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(getVar())){
                	pageContext.setAttribute(getVar(), prop);
                }else{
                	pageContext.getOut().print(prop);
                }
            } catch (Exception ex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(Messages.get().getBundle().key(Messages.ERR_PROCESS_TAG_1, "localizeProperty"), ex);
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
        m_defaultValue = null;
        m_file = null;
        m_locale = null;
        m_propertyName = null;
        m_varName = null;
    }
    
    
	public String getDefault() {
		return CmsStringUtil.isNotEmptyOrWhitespaceOnly(m_defaultValue) ? m_defaultValue : "";
	}
	
	public String getFile() {
		return CmsStringUtil.isNotEmptyOrWhitespaceOnly(m_file) ? m_file : "parent";
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
	
	
	
	

	public void setDefault(String defaultValue) {
		this.m_defaultValue = defaultValue;
	}
	
	public void setFile(String file) {
		this.m_file = file;
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

	
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			testMain(null);
			testMain("");
			testMain(" ");
			testMain("n'importe quoi");
			testMain("fr:francais");
			testMain("fr=francais");
			testMain("jp=japon");
			testMain("en=english");
			testMain("fritre|en=My t:Mein titel");
			testMain("fr=Mon titre|en:My title|de=Mein titel");
			testMain("fr=Mon titre|en=My title|de=Mein titel");
		} catch (CmsException e) {
			e.printStackTrace();
		}

	}
	
	private static void testMain(String value) throws CmsException{
		boolean isLocalizable = isLocalizableFormatted(value);
		String resultFr = extractLocalizedValue(value,"fr");
		String resultEn = extractLocalizedValue(value,"en");
		String resultEmpty = extractLocalizedValue(value,null);
		
		System.out.println(value);
		System.out.println("  isLocalizable = "+isLocalizable);
		System.out.println("  extract FR = "+resultFr);
		System.out.println("  extract EN = "+resultEn);
		System.out.println("  extract Empty = "+resultEmpty);
	}
	
}
