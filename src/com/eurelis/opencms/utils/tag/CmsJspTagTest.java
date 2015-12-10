/**
 * 
 */
package com.eurelis.opencms.utils.tag;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.opencms.flex.CmsFlexController;
import org.opencms.jsp.Messages;
import org.opencms.main.CmsLog;

/**
 * This tag is used to test if the taglib is interpreted.<p>
 * <code>&lt;utils:test /&gt;</code> display <code>TAG TEST OK</code> if taglib is interpreted.
 * 
 * @author Sandrine Prousteau
 * @version 0.0.1
 * @since com.eurelis.opencms.utils 1.0.0.0
 */
public class CmsJspTagTest extends TagSupport {

	/** Serial version UID required for safe serialization. */
	private static final long serialVersionUID = -5492124474290961127L;
	
	/** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspTagTest.class);
    
    
    /**
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {

        return EVAL_PAGE;
    }
    
    /**
     * @return SKIP_BODY
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {
    	
    	//if(LOG.isDebugEnabled()) LOG.debug("doStartTag TAG TEST !!!");
    	
    	ServletRequest req = pageContext.getRequest();

        // This will always be true if the page is called through OpenCms 
        if (CmsFlexController.isCmsRequest(req)) {

            try {
                
                pageContext.getOut().print("TAG TEST OK");

            } catch (Exception ex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(Messages.get().getBundle().key(Messages.ERR_PROCESS_TAG_1, "test"), ex);
                }
                throw new javax.servlet.jsp.JspException(ex);
            }
        }
        
    	return SKIP_BODY;
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	}

}
