package com.eurelis.opencms.utils.coreextensions;

import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.opencms.ade.configuration.CmsADEConfigData;
import org.opencms.ade.configuration.CmsADEManager;
import org.opencms.ade.detailpage.CmsDetailPageInfo;
import org.opencms.ade.detailpage.I_CmsDetailPageFinder;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsVfsException;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.file.types.CmsResourceTypeImage;
import org.opencms.loader.CmsLoaderException;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.site.CmsSite;
import org.opencms.staticexport.CmsDefaultLinkSubstitutionHandler;
import org.opencms.staticexport.CmsLinkManager;
import org.opencms.staticexport.CmsStaticExportManager;
import org.opencms.staticexport.Messages;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.CmsWorkplace;

/**
 * This class is used to upgrade OpenCms default class, in order to get the details page of items even if the items are in subfolders of defined type folder.<p>
 * 
 * 
 * To use this class, modify on the file /WEB-INF/config/opencms-importexport.xml the tab linksubstitutionhandler :
 * &lsaquo;staticexport enabled="true">
 *   &lsaquo;staticexporthandler>org.opencms.staticexport.CmsOnDemandStaticExportHandler&lsaquo;/staticexporthandler>
 *   <!--&lsaquo;linksubstitutionhandler>org.opencms.staticexport.CmsDefaultLinkSubstitutionHandler&lsaquo;/linksubstitutionhandler>-->
 *   &lsaquo;linksubstitutionhandler>com.eurelis.opencms.utils.coreextensions.CmsLinkSubstitutionHandler&lsaquo;/linksubstitutionhandler>
 *   ...
 * 
 * 
 * 
 * @author Sandrine Prousteau
 * @version 0.0.1
 * @since com.eurelis.opencms.utils 1.0.1.2
 */
public class CmsLinkSubstitutionHandler extends CmsDefaultLinkSubstitutionHandler{

	/** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsLinkSubstitutionHandler.class);
    
    
    
    
    public String getLink(CmsObject cms, String link, String siteRoot, boolean forceSecure) {

        LOG.debug("getLink - [" + link + "]");
        LOG.debug("getLink ~ [" + link + "] with siteRoot = " + siteRoot + " forceSecure = " + forceSecure);
        
        String siteRootCms = cms.getRequestContext().getSiteRoot();
        LOG.debug("getLink ~ [" + link + "] contextSiteRoot: " + siteRootCms);
        
        
        if (CmsStringUtil.isEmpty(link)) {
            // not a valid link parameter, return an empty String
            return "";
        }
        // make sure we have an absolute link        
        String absoluteLink = CmsLinkManager.getAbsoluteUri(link, cms.getRequestContext().getUri());
        LOG.debug("getLink ~ [" + link + "] absoluteLink: " + absoluteLink);

        String vfsName;
        String parameters;
        // check if the link has parameters, if so cut them
        int pos = absoluteLink.indexOf('?');
        if (pos >= 0) {
            vfsName = absoluteLink.substring(0, pos);
            parameters = absoluteLink.substring(pos);
        } else {
            vfsName = absoluteLink;
            parameters = null;
        }
        LOG.debug("getLink ~ [" + link + "] parameters&anchor: " + parameters);

        // check for anchor
        String anchor = null;
        pos = vfsName.indexOf('#');
        if (pos >= 0) {
            anchor = vfsName.substring(pos);
            vfsName = vfsName.substring(0, pos);
        }
        LOG.debug("getLink ~ [" + link + "] onlyanchor: " + anchor);
        LOG.debug("getLink - [" + link + "] vfsName: " + vfsName);

        String resultLink = null;
        String uriBaseName = null;
        boolean useRelativeLinks = false;

        // determine the target site of the link        
        CmsSite currentSite = OpenCms.getSiteManager().getCurrentSite(cms);
        CmsSite targetSite = null;
        if (CmsStringUtil.isNotEmpty(siteRoot)) {
            targetSite = OpenCms.getSiteManager().getSiteForSiteRoot(siteRoot);
        }
        if (targetSite == null) {
            targetSite = currentSite;
        }
        LOG.debug("getLink ~ [" + link + "] currentSite: " + currentSite);
        LOG.debug("getLink ~ [" + link + "] targetSite: " + targetSite);

        String targetSiteRoot = targetSite.getSiteRoot();
        LOG.debug("getLink - [" + link + "] targetSiteRoot: " + targetSiteRoot);
        String originalVfsName = vfsName;
        String detailPage = null;
        try {
            String rootVfsName;
            if (!vfsName.startsWith(targetSiteRoot)
                && !vfsName.startsWith(CmsResource.VFS_FOLDER_SYSTEM + "/")
                && !OpenCms.getSiteManager().startsWithShared(vfsName)) {
                rootVfsName = CmsStringUtil.joinPaths(targetSiteRoot, vfsName);
            } else {
                rootVfsName = vfsName;
            }
            LOG.debug("getLink - [" + link + "] rootVfsName: " + rootVfsName);
            if (!rootVfsName.startsWith(CmsWorkplace.VFS_PATH_WORKPLACE)) {
            	// never use the ADE manager for workplace links, to be sure the workplace stays usable in case of configuration errors
                I_CmsDetailPageFinder finder = OpenCms.getADEManager().getDetailPageFinder();
                detailPage = finder.getDetailPage(cms, rootVfsName, cms.getRequestContext().getUri());
                try{
                	if(cms.existsResource(vfsName) && cms.existsResource(cms.getRequestContext().removeSiteRoot(rootVfsName))){
                	    LOG.debug("getLink - [" + link + "] rootVfsName as removed site root = " + cms.getRequestContext().removeSiteRoot(rootVfsName));
                		int typeId = cms.readResource(cms.getRequestContext().removeSiteRoot(rootVfsName)).getTypeId();
                		LOG.debug("getLink - [" + link + "] detailPage = "+detailPage+" rootVfsName = " + rootVfsName + " typeId = " + typeId);
                		
    	                if(detailPage==null && /*typeId>10000 && */!vfsName.equals("/permalink/")){
    	                	String typeName = OpenCms.getResourceManager().getResourceType(typeId).getTypeName();
    	                	LOG.debug("getLink ~ typeName = "+typeName);
    	                    CmsADEManager ade = OpenCms.getADEManager();
    	                    //contextuel
    	                    CmsADEConfigData configDataUri = ade.lookupConfiguration(cms, cms.getRequestContext().getUri());
    	                    List<CmsDetailPageInfo> pageInfoUri = configDataUri.getDetailPagesForType(typeName);
    	                    if ((pageInfoUri == null) || pageInfoUri.isEmpty()) {
    	                    	LOG.debug("getLink ~ pageInfoUri null empty for type "+typeName + " and path " + cms.getRequestContext().getUri());
    	                    	
    	                    	CmsADEConfigData configDataRootUri = ade.lookupConfiguration(cms, cms.getRequestContext().addSiteRoot(cms.getRequestContext().getUri()));
    	                        List<CmsDetailPageInfo> pageInfoRootUri = configDataRootUri.getDetailPagesForType(typeName);
    	                        if ((pageInfoRootUri == null) || pageInfoRootUri.isEmpty()) {
    	                        	LOG.debug("getLink ~ pageInfoRootUri null empty for type "+typeName + " and path " + cms.getRequestContext().addSiteRoot(cms.getRequestContext().getUri()));
    	                        }else{
    	                    		detailPage = pageInfoRootUri.get(0).getUri();
    	                        	if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(detailPage)) {
    	                        		detailPage = cms.getRequestContext().removeSiteRoot(detailPage);
    	                        		LOG.debug("getLink ~ detailPage = "+detailPage);
    	                        	}
    	                    	}
    	                    }else{
    	                    	detailPage = pageInfoUri.get(0).getUri();
    	                    	if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(detailPage)) {
    	                    		detailPage = cms.getRequestContext().removeSiteRoot(detailPage);
    	                    		LOG.debug("getLink ~ detailPage = "+detailPage);
    	                    	}
    	                    }
    	                }
                	}else{
                	    LOG.debug("getLink - [" + link + "] vfsName "+ vfsName+ " or " + cms.getRequestContext().removeSiteRoot(rootVfsName) + " doesn't exists, dont't check detailsPage");
                	}                	
                }catch(Exception e){
                	LOG.error("Error in custom behaviour : " + e);
                }
            }
            if (detailPage != null) {
                if (detailPage.startsWith(targetSiteRoot)) {
                    detailPage = detailPage.substring(targetSiteRoot.length());
                    if (!detailPage.startsWith("/")) {
                        detailPage = "/" + detailPage;
                    }
                }
                LOG.debug("getLink - [" + link + "] detailPage: " + detailPage);
                try {
                    CmsResource element = null;
                    if(CmsStringUtil.isEmptyOrWhitespaceOnly(siteRootCms)){
                        siteRootCms = "/";
                        cms.getRequestContext().setSiteRoot("/");
                        LOG.debug("getLink - [" + link + "] change siteRoot to: " + siteRootCms);
                        
                        LOG.debug("getLink - [" + link + "] element to check: " + targetSiteRoot + vfsName);
                        element = cms.readResource(targetSiteRoot + vfsName);
                        LOG.debug("getLink - [" + link + "] element: " + element.getRootPath());
                    }else{
                        LOG.debug("getLink - [" + link + "] element to check: " + vfsName);
                        element = cms.readResource(vfsName);
                        LOG.debug("getLink - [" + link + "] element: " + element.getRootPath());
                    }
                    Locale locale = cms.getRequestContext().getLocale();
                    List<Locale> defaultLocales = OpenCms.getLocaleManager().getDefaultLocales();
                    vfsName = CmsStringUtil.joinPaths(
                        detailPage,
                        cms.getDetailName(element, locale, defaultLocales),
                        "/");
                } catch (CmsVfsException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        } catch (CmsVfsResourceNotFoundException e) {
            LOG.error("vfsName = " + vfsName + " --- " + e.getLocalizedMessage(), e);
        } catch (CmsException e) {
            LOG.error("vfsName = " + vfsName + " --- " + e.getLocalizedMessage(), e);
        }

        // if the link points to another site, there needs to be a server prefix
        String serverPrefix;
        if (targetSite != currentSite) {
            serverPrefix = targetSite.getUrl();
        } else {
            serverPrefix = "";
        }

        // in the online project, check static export and secure settings
        if (cms.getRequestContext().getCurrentProject().isOnlineProject()) {
            // first check if this link needs static export
            CmsStaticExportManager exportManager = OpenCms.getStaticExportManager();
            String oriUri = cms.getRequestContext().getUri();
            // check if we need relative links in the exported pages
            if (exportManager.relativeLinksInExport(cms.getRequestContext().getSiteRoot() + oriUri)) {
                // try to get base URI from cache  
                String cacheKey = exportManager.getCacheKey(targetSiteRoot, oriUri);
                uriBaseName = exportManager.getCachedOnlineLink(cacheKey);
                if (uriBaseName == null) {
                    // base not cached, check if we must export it
                    if (exportManager.isExportLink(cms, oriUri)) {
                        // base URI must also be exported
                        uriBaseName = exportManager.getRfsName(cms, oriUri);
                    } else {
                        // base URI dosn't need to be exported
                        uriBaseName = exportManager.getVfsPrefix() + oriUri;
                    }
                    // cache export base URI
                    exportManager.cacheOnlineLink(cacheKey, uriBaseName);
                }
                // use relative links only on pages that get exported
                useRelativeLinks = uriBaseName.startsWith(OpenCms.getStaticExportManager().getRfsPrefix(
                    cms.getRequestContext().getSiteRoot() + oriUri));
            }

            String detailPagePart = detailPage == null ? "" : detailPage + ":";
            // check if we have the absolute VFS name for the link target cached
            // (We really need the target site root in the cache key, because different resources with the same site paths
            // but in different sites may have different export settings. It seems we don't really need the site root 
            // from the request context as part of the key, but we'll leave it in to make sure we don't break anything.)
            String cacheKey = cms.getRequestContext().getSiteRoot()
                + ":"
                + targetSiteRoot
                + ":"
                + detailPagePart
                + absoluteLink;
            resultLink = exportManager.getCachedOnlineLink(cacheKey);
            if (resultLink == null) {
                String storedSiteRoot = cms.getRequestContext().getSiteRoot();
                try {
                    cms.getRequestContext().setSiteRoot(targetSite.getSiteRoot());
                    // didn't find the link in the cache
                    if (exportManager.isExportLink(cms, vfsName)) {
                        // export required, get export name for target link
                        resultLink = exportManager.getRfsName(cms, vfsName, parameters);
                        // now set the parameters to null, we do not need them anymore
                        parameters = null;
                    } else {
                        // no export required for the target link
                        resultLink = exportManager.getVfsPrefix().concat(vfsName);
                        // add cut off parameters if required
                        if (parameters != null) {
                            resultLink = resultLink.concat(parameters);
                        }
                    }
                } finally {
                    cms.getRequestContext().setSiteRoot(storedSiteRoot);
                }
                // cache the result
                exportManager.cacheOnlineLink(cacheKey, resultLink);
            }

            // now check for the secure settings 

            // check if either the current site or the target site does have a secure server configured
            if (targetSite.hasSecureServer() || currentSite.hasSecureServer()) {

                if (!vfsName.startsWith(CmsWorkplace.VFS_PATH_SYSTEM)
                    && !OpenCms.getSiteManager().startsWithShared(vfsName)) {
                    // don't make a secure connection to the "/system" folder (why ?)
                    int linkType = -1;
                    try {
                        // read the linked resource 
                        linkType = cms.readResource(originalVfsName).getTypeId();
                    } catch (CmsException e) {
                        // the resource could not be read
                        if (LOG.isInfoEnabled()) {
                            String message = Messages.get().getBundle().key(
                                Messages.LOG_RESOURCE_ACESS_ERROR_3,
                                vfsName,
                                cms.getRequestContext().getCurrentUser().getName(),
                                cms.getRequestContext().getSiteRoot());
                            if (LOG.isDebugEnabled()) {
                                LOG.debug(message, e);
                            } else {
                                LOG.info(message);
                            }
                        }
                    }

                    // images are always referenced without a server prefix
                    int imageId;
                    try {
                        imageId = OpenCms.getResourceManager().getResourceType(CmsResourceTypeImage.getStaticTypeName()).getTypeId();
                    } catch (CmsLoaderException e1) {
                        // should really never happen
                        LOG.warn(e1.getLocalizedMessage(), e1);
                        imageId = CmsResourceTypeImage.getStaticTypeId();
                    }
                    if (linkType != imageId) {
                        // check the secure property of the link
                        boolean secureRequest = exportManager.isSecureLink(cms, oriUri);
                        boolean secureLink = exportManager.isSecureLink(
                            cms,
                            vfsName,
                            targetSite.getSiteRoot(),
                            secureRequest);
                        // if we are on a normal server, and the requested resource is secure, 
                        // the server name has to be prepended                        
                        if (secureLink && (forceSecure || !secureRequest)) {
                            serverPrefix = targetSite.getSecureUrl();
                        } else if (!secureLink && secureRequest) {
                            serverPrefix = targetSite.getUrl();
                        }
                    }
                }
            }
            // make absolute link relative, if relative links in export are required
            // and if the link does not point to another server
            if (useRelativeLinks && CmsStringUtil.isEmpty(serverPrefix)) {
                resultLink = CmsLinkManager.getRelativeUri(uriBaseName, resultLink);
            }

        } else {
            // offline project, no export or secure handling required
            if (OpenCms.getRunLevel() >= OpenCms.RUNLEVEL_3_SHELL_ACCESS) {
                // in unit test this code would fail otherwise
                resultLink = OpenCms.getStaticExportManager().getVfsPrefix().concat(vfsName);
            }

            // add cut off parameters and return the result
            if ((parameters != null) && (resultLink != null)) {
                resultLink = resultLink.concat(parameters);
            }
        }

        if ((anchor != null) && (resultLink != null)) {
            resultLink = resultLink.concat(anchor);
        }

        return serverPrefix.concat(resultLink);
    }
	
}
