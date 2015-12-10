package com.eurelis.opencms.utils.function;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsResource;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.jsp.util.CmsJspElFunctions;
import org.opencms.jsp.util.CmsJspVfsAccessBean;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.util.CmsStringUtil;

/**
 * Provides utility methods that allow convenient access to the OpenCms VFS, indented to be used from a JSP with the JSTL or EL.
 * 
 * @author Sandrine Prousteau
 * @version 0.0.2
 * @since com.eurelis.opencms.utils 1.0.1.2
 */
public class CmsJspUtilsFunctions {

	/** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspUtilsFunctions.class);
	
	/**
     * Hide the public constructor.<p>
     */
    private CmsJspUtilsFunctions() {

        // NOOP
    }
    
    
    /**
     * Test if the provided uri is an existing internal file, even if contains parameters or anchors, and return his contents as String.<p>
     * Return null if file is not found, or if it's a folder.
     * 
     * Use as :
     * <code>${utils:fileContentAsString(pageContext,pointerPath)}</code>
     * 
     * @param cmsObject
     * @param uri
     * @return
     * @throws CmsException 
     * @since com.eurelis.opencms.utils 1.0.1.2
     */
    public static String fileContentAsString(Object cmsObject, Object uri) throws CmsException{
    	
    	String result = null;
    	
    	if(cmsObject!=null){
    		CmsJspVfsAccessBean vfs = CmsJspVfsAccessBean.create(CmsJspElFunctions.convertCmsObject(cmsObject));
    		
    		if(vfs!=null && uri!=null){
    			//transform uri en String
    			uri = uri.toString();
        		if (uri!=null && uri instanceof String) {
            		String value = (String) uri;
            		
            		if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(value)){
            			LOG.debug("'fileContentAsString' function called with a String value " + value + ".");
            			
            			//suppression des parametres d'URI
                        if(value.contains("?")){
                        	value = value.substring(0, value.indexOf("?"));
                        	LOG.debug("   The String value containing '?'. Substring is " + value + ".");
                        }else if(value.contains("#")){
                        	value = value.substring(0, value.indexOf("#"));
                        	LOG.debug("   The String value containing '#'. Substring is " + value + ".");
                        }
                        
                        if(vfs.getCmsObject().existsResource(value)){
                        	
                        	CmsResource rsc = vfs.getCmsObject().readResource(value);
                        	if(rsc!=null && rsc.isFile()){
                        		
                        		CmsFile file = vfs.getCmsObject().readFile(rsc);
                        		result = new String(file.getContents()).trim();
                        		
                        	}else{
                        		LOG.debug("'fileContentAsString' function called with a resource null or a folder.");
                        	}
                        	
                        }else{
                        	LOG.debug("'fileContentAsString' function called with a non existing resource.");
                        }
            			
            		}else{
            			LOG.debug("'fileContentAsString' function called with an empty value.");
            		}
            		
                } else {
                    LOG.warn("'fileContentAsString' function called with a non String value. (" + uri.toString() + ")");
                }
        	}else{
        		LOG.warn("'fileContentAsString' function called with a null uri or a null vfs.");
        	}
    	}else{
    		LOG.error("'fileContentAsString' function called with a null cmsObject.");
    	}
    	
    	return result;
    	
    }
    
    
    /**
     * Test if the provided uri is an existing internal file, even if contains parameters or anchors.<p>
     * 
     * Use as :
     * <code><c:if test="${utils:existsUri(pageContext,imagePath)}"></code>
     * 
     * @param cmsObject the Object to create a CmsObject from
     * @param uri the Object to check the uri from 
     * @return true of false
     * @since com.eurelis.opencms.utils 1.0.1.2
     */
    public static boolean existsUri(Object cmsObject, Object uri){
    	
    	boolean result = false;
    	
    	if(cmsObject!=null){
    		CmsJspVfsAccessBean vfs = CmsJspVfsAccessBean.create(CmsJspElFunctions.convertCmsObject(cmsObject));
    		
    		if(vfs!=null && uri!=null){
    			//transform uri en String
    			uri = uri.toString();
    			if (uri!=null && uri instanceof String) {
            		String value = (String) uri;
            		
            		if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(value)){
            			LOG.debug("'existsUri' function called with a String value " + value + ".");
            			
            			//suppression des parametres d'URI
                        if(value.contains("?")){
                        	value = value.substring(0, value.indexOf("?"));
                        	LOG.debug("   The String value containing '?'. Substring is " + value + ".");
                        }else if(value.contains("#")){
                        	value = value.substring(0, value.indexOf("#"));
                        	LOG.debug("   The String value containing '#'. Substring is " + value + ".");
                        }
                        
                        if(vfs.getCmsObject().existsResource(value)){
                        	result = true;
                        }
            			
            		}else{
            			LOG.debug("'existsUri' function called with an empty value.");
            		}
            		
                } else {
                    LOG.warn("'existsUri' function called with a non String value. (" + uri.toString() + ")");
                }
        	}else{
        		LOG.warn("'existsUri' function called with a null uri or a null vfs.");
        	}
    	}else{
    		LOG.error("'existsUri' function called with a null cmsObject.");
    	}
    	
    	return result;
    }
    
    
    /**
     * Return the crop params from the __scale param of an image path.<p>
     * 
     * Return something like : <code>",cx:"+cx+",cy:"+cy+",ch:"+ch+",cw:"+cw</code> or empty
     * 
     * @param scale the __scale param of an image path
     * @return
     * @since com.eurelis.opencms.utils 1.0.1.3
     */
    public static String getCrop(Object scaleParams){
    	
    	String value = (String) scaleParams.toString();
    	
    	if(CmsStringUtil.isEmptyOrWhitespaceOnly(value)){
    		return "";
    	}else{
    		Map map = CmsStringUtil.splitAsMap(value, ",", ":");
    		String cx = "";
			String cy = "";
			String ch = "";
			String cw = "";
			
			if (map!=null && !map.isEmpty() && map.containsKey("cx")) cx = (String)map.get("cx");
			if (map!=null && !map.isEmpty() && map.containsKey("cy")) cy = (String)map.get("cy");
			if (map!=null && !map.isEmpty() && map.containsKey("ch")) ch = (String)map.get("ch");
			if (map!=null && !map.isEmpty() && map.containsKey("cw")) cw = (String)map.get("cw");
			
			if(CmsStringUtil.isEmptyOrWhitespaceOnly(cx)){
				return "";
			}else{
				return ",cx:"+cx+",cy:"+cy+",ch:"+ch+",cw:"+cw;
			}
    		
    	}
    	
    }
    
    
    /**
     * Return href link attribute, depending on the type id of the resource.<p>
     * 
     * <li>If type is 5 (pointer), get the content of the pointer as href attribute, adding "http://" if missing.
     * <li>If type is 2 (binary), get the OpenCms link for this path.
     * <li>Else, get the Eurelis navigationHref link searched with this path. See the tag utils:navigationHref.
     * 
     * Use as:<br/>
     * <code>
     * < jsp:useBean id="cmsAction" class="org.opencms.jsp.CmsJspActionElement">
  	 * <% cmsAction.init(pageContext, request, response); %>
	 * </jsp:useBean>
	 * < c:set var="cmsAction" value="<%=cmsAction %>" />
     * < c:set var="href" value="${utils:getHref(pageContext,Page,cmsAction)}" />
     * < a href="${href}">...
     * </code>
     * 
     * @param cmsObject the Object to create a CmsObject from
     * @param uri the path of the resource
     * @param bean
     * @return
     * @throws CmsException
     * @since com.eurelis.opencms.utils 1.0.1.3
     */
    public static String getHref(Object cmsObject, Object uri, Object bean) throws CmsException{
        
    	String result = null;
    	
    	if(cmsObject!=null){
    		CmsJspVfsAccessBean vfs = CmsJspVfsAccessBean.create(CmsJspElFunctions.convertCmsObject(cmsObject));
    		
    		if(vfs!=null && uri!=null && bean!=null){
    			uri = uri.toString();
        		if (uri!=null && uri instanceof String && bean instanceof CmsJspActionElement) {
        			String value = (String) uri;
        			CmsJspActionElement b = (CmsJspActionElement) bean;
        			if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(value)){
        				
        				//suppression des parametres d'URI
        				String parsedValue = value;
                        if(parsedValue.contains("?")){
                        	parsedValue = parsedValue.substring(0, value.indexOf("?"));
                        	LOG.debug("   The String value containing '?'. Substring is " + parsedValue + ".");
                        }else if(parsedValue.contains("#")){
                        	parsedValue = parsedValue.substring(0, parsedValue.indexOf("#"));
                        	LOG.debug("   The String value containing '#'. Substring is " + parsedValue + ".");
                        }
        				
        				if(vfs.getCmsObject().existsResource(parsedValue)){
            				int typeID = vfs.getCmsObject().readResource(parsedValue).getTypeId();
            				switch(typeID){
            					case 5 : 	//pointer
            						String pointerContent = com.eurelis.opencms.utils.function.CmsJspUtilsFunctions.fileContentAsString(cmsObject, parsedValue);
            						if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(pointerContent)){
            							if(pointerContent.matches("^(https?|mms|ftp|rtsp)://")){
            								result = pointerContent;
            							}else{
            								result = "http://" + pointerContent;
            							}
            						}else{
            							LOG.warn("'getTarget' function called with an empty pointer. (" + uri.toString() + ")");
            						}
            						break;
            					case 2 :	//binary
            						result = vfs.getLink().get(value);
            						break;
            					default :
            						String navHref = com.eurelis.opencms.utils.tag.CmsJspTagNavigationHref.navigationHrefAction(value, new Boolean(true), b);
            						if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(navHref)){
            							result = vfs.getLink().get(navHref);
            							result = b.link(navHref);
            							LOG.debug("   vfs.getLink().get(navHref) : " + vfs.getLink().get(navHref));
            							LOG.debug("   b.link(navHref) : " + b.link(navHref));
            						}
            				}
            			}else{
            				LOG.error("'getTarget' function called with an inexistant path. (" + uri.toString() + ")");
            			}
        				
        			}else{
        				LOG.warn("'getTarget' function called with an empty value.");
        			}
        		}else {
                    LOG.warn("'getTarget' function called with a non String value. (" + uri.toString() + ")");
                }
    		}else{
        		LOG.warn("'getTarget' function called with a null uri or a null vfs.");
        	}
    		
    	}else{
    		LOG.error("'getTarget' function called with a null cmsObject.");
    	}
    	
    	return result;
    	
    }
    
    
    public static int getHrefTypeId(Object cmsObject, Object uri, Object bean) throws CmsException{
        
    	int result = -1;
    	
    	if(cmsObject!=null){
    		CmsJspVfsAccessBean vfs = CmsJspVfsAccessBean.create(CmsJspElFunctions.convertCmsObject(cmsObject));
    		if(vfs!=null && uri!=null && bean!=null){
    			uri = uri.toString();
        		if (uri!=null && uri instanceof String && bean instanceof CmsJspActionElement) {
        			String value = (String) uri;
        			CmsJspActionElement b = (CmsJspActionElement) bean;
        			if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(value)){
        				if(vfs.getCmsObject().existsResource(value)){
            				int typeID = vfs.getCmsObject().readResource(value).getTypeId();
            				switch(typeID){
            					case 5 : 	//pointer
            						result = 5;
            						break;
            					case 2 :	//binary
            						result = 2;
            						break;
            					case 3 :	//image
            						result = 3;
            						break;
            					default :
            						String navHref = com.eurelis.opencms.utils.tag.CmsJspTagNavigationHref.navigationHrefAction(value, new Boolean(true), b);
            						if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(navHref)){
            							result = vfs.getCmsObject().readResource(navHref).getTypeId();
            						}
            				}
            			}else{
            				LOG.error("'getHrefTypeId' function called with an inexistant path. (" + uri.toString() + ")");
            			}
        			}else{
        				LOG.warn("'getHrefTypeId' function called with an empty value.");
        			}
        		}else {
                    LOG.warn("'getHrefTypeId' function called with a non String value. (" + uri.toString() + ")");
                }
    		}else{
        		LOG.warn("'getHrefTypeId' function called with a null uri or a null vfs.");
        	}
    	}else{
    		LOG.error("'getHrefTypeId' function called with a null cmsObject.");
    	}
    	
    	return result;
    	
    }
    
    
    /**
     * Return the image path rescaled.<p>
     * 
     * Type = 1<br/>
     * Quality = 90<br/>
     * Color = transparent<br/>
     * <br/>
     * Use as : <br/>
     * <code>&lt;c:if test="${cms.container.type == 'home66'}">&lt;c:set var="Image">${utils:getRescaledImagePath(Image, 720, 340)}&lt;/c:set>&lt;/c:if></code><br/>
     * <code>&lt;c:if test="${cms.container.type == 'home33'}">&lt;c:set var="Image">${utils:getRescaledImagePath(Image, 233, '')}&lt;/c:set>&lt;/c:if></code>
     * 
     * TODO : in image scale in null, get the property dimensions, and calculate scale in case only width or only height is asked.
     * 
     * @param path the image path, with ou without scale params
     * @param width the width to set, or empty
     * @param height the height to set, or empty
     * @return the image path rescaled
     * @since com.eurelis.opencms.utils 1.0.1.3
     */
    public static String getRescaledImagePath(Object path, Object width, Object height){
    	
    	try{
    		String valuePath = (String) path.toString();
        	String valueWidth = (String) width.toString();
        	String valueHeight = (String) height.toString();
        	
        	String type = "1";
        	String quality = "90";
        	String color = "transparent";
        	
        	if(valuePath==null || valuePath.equalsIgnoreCase("")){
        		LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : empty");
        		return "";
        	}else{
        		String valueScale = null;
        		if(valuePath.contains("?")){
        			valueScale = CmsJspElFunctions.getRequestParam(valuePath,"__scale");
        		}
        		LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : scale from path = "+valueScale);
        		
        		if(valueScale==null || valueScale.equalsIgnoreCase("") || valueScale.equalsIgnoreCase("null")){
        			
        			if((valueWidth==null || valueWidth.equalsIgnoreCase("")) && (valueHeight==null || valueHeight.equalsIgnoreCase(""))){
        				LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : no image scale, no params scale => "+path);
        				return valuePath;
        			}else{
        				// HERE, must define width AND height!!
        				String result = CmsJspElFunctions.getRequestLink(valuePath) + "?__scale=" + "h:"+valueHeight+",w:"+valueWidth + ",t:"+type+",q:"+quality+",c:"+color;
        				LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : no image scale, but at least one params scale => "+result);
        				return result;
        			}
            		
            	}else{
            		
            		if((valueWidth==null || valueWidth.equalsIgnoreCase("")) && (valueHeight==null || valueHeight.equalsIgnoreCase(""))){
            			LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : image scale, and no params scale => "+path);
            			return valuePath;
            		}else if(valueWidth==null || valueWidth.equalsIgnoreCase("")){
            			Map map = CmsStringUtil.splitAsMap(valueScale, ",", ":");
                		String h = "";
            			String w = "";
            			if (map!=null && !map.isEmpty() && map.containsKey("h")) h = (String)map.get("h");
            			if (map!=null && !map.isEmpty() && map.containsKey("w")) w = (String)map.get("w");
            			if(h==null || h.equalsIgnoreCase("") || w==null || w.equalsIgnoreCase("")){
            				LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : image scale, and no width params scale => "+path);
                			return valuePath;
            			}else{
            				int intH = (new Integer(h.toString())).intValue();
                			int intW = (new Integer(w.toString())).intValue();
                			int intHeight = (new Integer(valueHeight.toString())).intValue();
                			int intWidth = intHeight * intW / intH;
                			
                			String result = CmsJspElFunctions.getRequestLink(valuePath) + "?__scale=" + "h:"+intHeight+",w:"+intWidth + getCrop(valueScale) + ",t:"+type+",q:"+quality+",c:"+color;
                			LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : image scale, and no width params scale => "+result);
                			return result;
            			}
            		}else if(valueHeight==null || valueHeight.equalsIgnoreCase("")){
            			Map map = CmsStringUtil.splitAsMap(valueScale, ",", ":");
                		String h = "";
            			String w = "";
            			if (map!=null && !map.isEmpty() && map.containsKey("h")) h = (String)map.get("h");
            			if (map!=null && !map.isEmpty() && map.containsKey("w")) w = (String)map.get("w");
            			if(h==null || h.equalsIgnoreCase("") || w==null || w.equalsIgnoreCase("")){
            				LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : image scale, and no height params scale => "+path);
                			return valuePath;
            			}else{
            				int intH = (new Integer(h.toString())).intValue();
                			int intW = (new Integer(w.toString())).intValue();
                			int intWidth = (new Integer(valueWidth.toString())).intValue();
                			int intHeight = intWidth * intH / intW;
                			
                			String result = CmsJspElFunctions.getRequestLink(valuePath) + "?__scale=" + "h:"+intHeight+",w:"+intWidth + getCrop(valueScale) + ",t:"+type+",q:"+quality+",c:"+color;
                			LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : image scale, and no height params scale => "+result);
                			return result;
            			}
            		}else{
            			String result = CmsJspElFunctions.getRequestLink(valuePath) + "?__scale=" + "h:"+valueHeight+",w:"+valueWidth + ",t:"+type+",q:"+quality+",c:"+color;
        				LOG.debug("getRescaledImagePath ("+path+","+width+","+height+") : image scale, and params scale => "+result);
        				return result;
            		}
            		
            	}
        		
        	}
    	}catch(Exception e){
    		LOG.error(e);
    	}
    	return "";
    	
    }
    
    
    
    /**
     * Return _blank or empty, depending on the type id of the resource.<p>
     * If type id is 5 (pointer) or 2 (binary), return _blank, else, return empty string.
     * <br/>
     * Use as :
     * <pre>
     * < c:set var="Page">${item.value['AlignedBanner/Link']}< /c:set>
     * < c:set var="target" value="${utils:getTarget(pageContext,Page)}" />
     * </pre>
     * 
     * @param cmsObject the Object to create a CmsObject from
     * @param uri the path of the resource
     * @return "_blank" or empty string
     * @since com.eurelis.opencms.utils 1.0.1.3
     */
    public static String getTarget(Object cmsObject, Object uri) throws CmsException{
        
    	String result = null;
    	
    	if(cmsObject!=null){
    		CmsJspVfsAccessBean vfs = CmsJspVfsAccessBean.create(CmsJspElFunctions.convertCmsObject(cmsObject));
    		
    		if(vfs!=null && uri!=null){
    			uri = uri.toString();
        		if (uri!=null && uri instanceof String) {
        			String value = (String) uri;
        			
        			if(CmsStringUtil.isNotEmptyOrWhitespaceOnly(value)){
        				//suppression des parametres d'URI
                        if(value.contains("?")){
                        	value = value.substring(0, value.indexOf("?"));
                        	LOG.debug("   The String value containing '?'. Substring is " + value + ".");
                        }else if(value.contains("#")){
                        	value = value.substring(0, value.indexOf("#"));
                        	LOG.debug("   The String value containing '#'. Substring is " + value + ".");
                        }
        				
        				if(vfs.getCmsObject().existsResource(value)){
            				int typeID = vfs.getCmsObject().readResource(value).getTypeId();
            				switch(typeID){
            					case 5 : 	//pointer
            						result = "_blank";
            						break;
            					case 2 :	//binary
            						result = "_blank";
            						break;
            					default :
            						result = "";
            				}
            			}else{
            				LOG.error("'getTarget' function called with an inexistant path. (" + uri.toString() + ")");
            			}
        			}else{
        				LOG.warn("'getTarget' function called with an empty value.");
        			}
        			
        		}else {
                    LOG.warn("'getTarget' function called with a non String value. (" + uri.toString() + ")");
                }
    		}else{
        		LOG.warn("'getTarget' function called with a null uri or a null vfs.");
        	}
    		
    	}else{
    		LOG.error("'getTarget' function called with a null cmsObject.");
    	}
    	
    	return result;
    	
    }
    
    /**
     * Return the textarea content, where the "\n" are replaced by "<br/>".<p>
     * 
     * Use as : <br/>
     * <pre>
     * < div class="description" ${content.rdfa['Text']}>${utils:replaceTextarea(Text)}< /div>
     * </pre>
     *  
     * @param string the textarea content
     * @return the textarea content, where the "\n" are replaced by "<br/>"
     * @since com.eurelis.opencms.utils 1.0.1.3
     */
    public static String replaceTextarea(Object content){
    	
    	String value = (String) content.toString();
    	if(CmsStringUtil.isEmptyOrWhitespaceOnly(value)){
    		return value;
    	}else{
    		return value.replaceAll("\n", "<br/>");
    	}
    	
    }
    
}
