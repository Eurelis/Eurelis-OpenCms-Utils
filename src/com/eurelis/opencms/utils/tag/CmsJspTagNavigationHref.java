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
 * This tag is used to search the final href of a link,  specially if the file is a folder.<p>
 * It search for a filiation, recursively or not, in the order :
 * <ul>
 * <li>property default-file (used with the name of the subfile, not the path)
 * <li>subfile named "index"
 * <li>first subelement in navigation
 * </ul>
 * If nothing is found, return empty value.<p>
 * 
 * <code>&lt;utils:navigationHref file="${folderpath}" recursive="true" /&gt;</code>.
 * 
 * 
 * @author Sandrine Prousteau
 * @version 0.0.1
 * @since com.eurelis.opencms.utils 1.0.0.0
 */
public class CmsJspTagNavigationHref extends TagSupport{

	/** Serial version UID required for safe serialization. */
	private static final long serialVersionUID = 3302697820647029673L;

	/** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspTagNavigationHref.class);
    
    /** The file to read the href from. */
    private String m_hrefFile;
    
    /** True if search is recursive, false otherwise. */
    private Boolean m_recursive;

    
    
   
    
    
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
    public static String navigationHrefAction(
        String file,
        Boolean recursive,
        CmsJspActionElement bean) throws CmsException {

        String value = null;
        
        if(!bean.getCmsObject().existsResource(file)){
        	return null;
        }
        
        CmsResource rsc = bean.getCmsObject().readResource(file);
        if(rsc.isFile()){
        	return bean.getCmsObject().getRequestContext().removeSiteRoot(rsc.getRootPath());
        }else{
        	if(recursive==null){
        		recursive = new Boolean(true);
            }
        		
    		//default-file : lit la propriete default-file (y mettre juste le name)
    		CmsResource defaultFile = null;
    		String default_file = bean.getCmsObject().readPropertyObject(
    				bean.getCmsObject().getRequestContext().removeSiteRoot(rsc.getRootPath()), 
    				CmsPropertyDefinition.PROPERTY_DEFAULT_FILE, 
    				false).getValue();
    		String default_path = CmsResource.getFolderPath(bean.getCmsObject().getRequestContext().removeSiteRoot(rsc.getRootPath())) + default_file;
		    if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(default_file)){
		    	if(bean.getCmsObject().existsResource(default_path)){
		    		defaultFile = bean.getCmsObject().readResource(default_path);
		    	}
		    }

		    //fichier index.
		    if(defaultFile==null){
		    	List<CmsResource> all_sons = bean.getCmsObject().getFilesInFolder(bean.getCmsObject().getRequestContext().removeSiteRoot(rsc.getRootPath()));
    		    boolean existsIndex = false;
    		    Iterator<CmsResource> it = all_sons.iterator();
    		    while(it.hasNext() && !existsIndex){
    		    	CmsResource cmsResource = (CmsResource)it.next();
    		    	if(cmsResource.getName().equals("index") || cmsResource.getName().startsWith("index.")){
    		    		existsIndex = true;
    		    		defaultFile = cmsResource;
    		    	}
    		    }
		    }
    		
    		if(defaultFile!=null){
    			if(LOG.isDebugEnabled()) LOG.debug("navigationHrefAction - defaultFile : '" + defaultFile.getRootPath() + "'");
    			
    			value = bean.getCmsObject().getRequestContext().removeSiteRoot(defaultFile.getRootPath());
    		}else{
    			if(LOG.isDebugEnabled()) LOG.debug("navigationHrefAction - defaultFile : null");
    			
    			//readFirstElementInNavigation : cherche le 1er element dans la navigation
    			List<CmsJspNavElement> elements_in_navigation = bean.getNavigation().getNavigationForFolder(bean.getCmsObject().getRequestContext().removeSiteRoot(rsc.getRootPath()));
		        
    			if(!elements_in_navigation.isEmpty()){
		        	CmsJspNavElement navigationFirstElement = (CmsJspNavElement)elements_in_navigation.get(0);
		        	if(LOG.isDebugEnabled()) LOG.debug("navigationHrefAction - navigationFirstElement : " + navigationFirstElement.getResourceName());
		        	
		        	value = bean.getCmsObject().getRequestContext().removeSiteRoot(navigationFirstElement.getResourceName());
		        }else{
		        	if(LOG.isDebugEnabled()) LOG.debug("navigationHrefAction - navigationFirstElement : no element");
		        }
    		}
    		
    		//boucle si c'est un dossier et qu'on a la recursivite
    		if(value!=null && bean.getCmsObject().readResource(value).isFolder() && recursive){
    			value = navigationHrefAction(value,recursive,bean);
    		}
        	
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
    	
    	if(LOG.isDebugEnabled()) LOG.debug("");
    	if(LOG.isDebugEnabled()) LOG.debug("doStartTag navigationHref (with resource '" + getFile() + "' and recursivity '" + isRecursive() + "') ...");
    	
    	CmsJspActionElement bean = new CmsJspActionElement(pageContext, (HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse());

    	// This will always be true if the page is called through OpenCms 
    	ServletRequest req = pageContext.getRequest();
        if (CmsFlexController.isCmsRequest(req)) {

            try {
            	String href = navigationHrefAction(getFile(), isRecursive(), bean);
            	if(href==null){
            		href = "";
            	}
                pageContext.getOut().print(href);

            } catch (Exception ex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(Messages.get().getBundle().key(Messages.ERR_PROCESS_TAG_1, "navigationHref"), ex);
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
        m_hrefFile = null;
        m_recursive = true;
    }
    
    
    /**
     * Returns the file.<p>
     * 
     * @return the file
     */
    public String getFile() {

        return m_hrefFile != null ? m_hrefFile : "";
    }
    
    /**
     * Returns the level of recursivity.<p>
     * 
     * @return the recursivity
     */
    public Boolean isRecursive() {

        return m_recursive != null ? m_recursive : new Boolean(true);
    }
    
    /**
     * Sets the file.<p>
     * 
     * @param file the file
     */
    public void setFile(String file) {

        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(file)) {
        	m_hrefFile = file;
        }
    }
    
    /**
     * Sets the recursive state.<p>
     * 
     * @param recursive recursive state
     */
    public void setRecursive(Boolean recursive) {

        if (recursive != null) {
        	m_recursive = recursive;
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
