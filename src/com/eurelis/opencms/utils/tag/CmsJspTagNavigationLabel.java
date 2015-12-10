/**
 * 
 */
package com.eurelis.opencms.utils.tag;

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.flex.CmsFlexController;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.jsp.Messages;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.util.CmsStringUtil;

import org.opencms.jsp.CmsJspNavElement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This tag is used to search the final label of a link.<p>
 * It search in filename and properties, in the order :
 * <ul>
 * <li>filename
 * <li>value default
 * <li>property Title
 * <li>property NavLabel
 * <li>property NavText
 * <li>custom label
 * </ul>
 * 
 * <code>&lt;utils:navigationLabel file="${path}" default="${value}" /&gt;</code>.
 * 
 * 
 * @author Sandrine Prousteau
 * @version 0.0.1
 * @since com.eurelis.opencms.utils 1.0.0.2
 */
public class CmsJspTagNavigationLabel extends TagSupport{

	/** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspTagNavigationLabel.class);
    
    /** The file to read the label from. */
    private String m_labelFile;
    
    /** The default value. */
    private String m_default;
    
    /** The custom value. */
    private String m_custom;

    
    
   
    
    
    /**
     * Internal action method.<p>
     * 
     * @param file the file
     * @param recursive true if recursive
     * @param bean the current bean
     * 
     * @return the value of the href or empty if not found
     *      
     * @throws CmsException if something goes wrong
     */
    public static String navigationLabelAction(
        String file,
        String defaultvalue,
        String customvalue,
        CmsJspActionElement bean) throws CmsException {

        String value = null;
        
        if(!bean.getCmsObject().existsResource(file)){
        	return null;
        }
        
        if(CmsStringUtil.isEmptyOrWhitespaceOnly(file)){
        	if(LOG.isWarnEnabled()) LOG.warn("  file : null or empty ! [ custom='" + customvalue + "' defaultvalue='" + defaultvalue + "'");
            return null;
        }
        
        String navText = bean.getCmsObject().readPropertyObject(file, CmsPropertyDefinition.PROPERTY_NAVTEXT, false).getValue();
        
        String navLabel = bean.getCmsObject().readPropertyObject(file, "NavLabel", false).getValue();
        
        String title = bean.getCmsObject().readPropertyObject(file, CmsPropertyDefinition.PROPERTY_TITLE, false).getValue();
        
        String filename = CmsResource.getName(file);
        
        if(LOG.isDebugEnabled()) LOG.debug("  file : " + file + "[ custom='" + customvalue + "' navText='" + navText + "' navLabel='" + navLabel + "' title='" + title + "' defaultvalue='" + defaultvalue + "' filename='" + filename + "'");
        
        if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(filename))
        	value = filename;
        if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(defaultvalue))
        	value = defaultvalue;
        if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(title))
        	value = title;
        if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(navLabel))
        	value = navLabel;
        if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(navText))
        	value = navText;
        if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(customvalue))
        	value = customvalue;
       
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
    	
    	if(LOG.isDebugEnabled()) LOG.debug("");
    	if(LOG.isDebugEnabled()) LOG.debug("doStartTag navigationLabel (with resource '" + getFile() + "' and default '" + getDefault() + "' and custom '" + getCustom() + "') ...");
    	
    	CmsJspActionElement bean = new CmsJspActionElement(pageContext, (HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse());

    	// This will always be true if the page is called through OpenCms 
    	ServletRequest req = pageContext.getRequest();
        if (CmsFlexController.isCmsRequest(req)) {

            try {
            	String label = navigationLabelAction(getFile(), getDefault(), getCustom(), bean);
                pageContext.getOut().print(label);

            } catch (Exception ex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(Messages.get().getBundle().key(Messages.ERR_PROCESS_TAG_1, "navigationLabel"), ex);
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
        m_labelFile = null;
        m_default = null;
        m_custom = null;
    }
    
    
    /**
     * Returns the file.<p>
     * 
     * @return the file
     */
    public String getFile() {

        return m_labelFile != null ? m_labelFile : "";
    }
    
    /**
     * Returns the default value.<p>
     * 
     * @return the default value
     */
    public String getDefault() {

        return CmsStringUtil.isNotEmptyOrWhitespaceOnly(m_default) ? m_default : null;
    }
    
    /**
     * Returns the custom value.<p>
     * 
     * @return the custom value
     */
    public String getCustom() {

        return CmsStringUtil.isNotEmptyOrWhitespaceOnly(m_custom) ? m_custom : null;
    }
    
    /**
     * Sets the file.<p>
     * 
     * @param file the file
     */
    public void setFile(String file) {

        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(file)) {
        	m_labelFile = file;
        }
    }
    
    /**
     * Sets the default value.<p>
     * 
     * @param defaultvalue default value
     */
    public void setDefault(String defaultvalue) {

        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(defaultvalue)) {
        	m_default = defaultvalue;
        }
    }
    
    /**
     * Sets the custom value.<p>
     * 
     * @param customvalue default value
     */
    public void setCustom(String customvalue) {

        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(customvalue)) {
        	m_custom = customvalue;
        }
    }
    
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		/*try {
			
		} catch (CmsException e) {
			e.printStackTrace();
		}*/
		
	}

}
