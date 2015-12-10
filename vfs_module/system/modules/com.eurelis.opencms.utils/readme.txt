<div id="accordion">
	<h3>Presentation du module</h3>
	<div class="content">
		<ul>
		<li>Ensemble de tags et de fonctions jstl utilitaires, et de classes de surcharge du core</li>
		</ul>
	</div>
	<h3>Historique</h3>
	<div class="content">
		<ul>
		<li>1.0.1.3 : (SPR) Mai 2013 : Nouveaux tags et fonctions JSTL</li>
		<li>1.0.1.2 : (SPR) Fev 2013 : Dependance OpenCms 8.5.0, Corrections, Nouveaux tags, Nouvelle surcharge de classe </li>
		<li>1.0.1.0 : (SPR) : Init 1ere version </li>
		</ul>
	</div>
	<h3>Installation / Configuration</h3>
	<div class="content">
		<p><u>Declaration des Taglib</u> (/web-inf/web.xml)</p>
		<pre>
		&lt;taglib>
		  &lt;taglib-uri>http://www.eurelis.com/taglib/opencms/utils&lt;/taglib-uri>
		  &lt;taglib-location>/WEB-INF/eurelis-utils.tld&lt;/taglib-location>
		&lt;/taglib>
		</pre>
		<p><u>Appels des Taglib</u> (/web-inf/config/opencms-vfs.xml)</p>
		<pre>
		&lt;param name="taglib.utils">http://www.eurelis.com/taglib/opencms/utils&lt;/param>
		</pre>
		<p><u>Gestion personnalisee des liens</u> (/web-inf/config/opencms-importexport.xml)</p>
		<pre>
		&lt;staticexport enabled="true">
		  &lt;staticexporthandler>org.opencms.staticexport.CmsOnDemandStaticExportHandler&lt;/staticexporthandler>
		  &lt;!--<linksubstitutionhandler>org.opencms.staticexport.CmsDefaultLinkSubstitutionHandler&lt;/linksubstitutionhandler>-->
		  &lt;linksubstitutionhandler>com.eurelis.opencms.utils.coreextensions.CmsLinkSubstitutionHandler&lt;/linksubstitutionhandler>
		</pre>
	</div>	
	<h3>Contenu</h3>
	<div class="content">
		<p><u>Extensions du core OpenCms</u></p>
		<p><b>CmsLinkSubstitutionHandler [depuis 1.0.1.2]</b> :<br/>
		Cette classe permet de palier au probl&egrave;me de g&eacute;n&eacute;ration des liens dans le cas d'item avec pages d&eacute;taill&eacute;es: 
		quand les items sont r&eacute;partis dans des sous-dossiers du dossier d&eacute;fini comme stockage pour le type. 
		La compilation de cette classe a n&eacute;cessit&eacute; de passer la d&eacute;pendance du module &agrave; la version 8.5.0 d'OpenCms.</p>
		<p>&nbsp;</p>
		
		<p><u>Taglib</u></p>
		
		<p><b>utils:info [depuis 1.0.1.2]</b> :<br/>
		Ce tag "am&eacute;liore" le tag d'OpenCms cms:info, pour r&eacute;cup&eacute;rer correctement les Meta des items en pages d&eacute;taill&eacute;es : 
		lecture des champs d'&eacute;dition MetaTitle, MetaDescription, MetaKeywords. Dissociation de mapping UrlName.<br/>
		<pre>
		&lt;meta name="description" content="&lt;utils:info property="opencms.description" />" />
		</pre></p>
		<p>&nbsp;</p>
		
		<p><b>utils:localizeProperty [depuis 1.0.1.0]</b> :<br/>
		Permet d'extraire un libell&eacute; de la propri&eacute;t&eacute; d'une ressource.<br/>
		<pre>
		&lt;utils:localizeProperty var="alt" file="${fn:substringBefore(content.value['Image'],'?')}" name="Description" locale="${locale}" default="${defaultlabel}"/>
  	&lt;utils:localizeProperty var="title" file="${fn:substringBefore(content.value['Image'],'?')}" name="Title" locale="${locale}" default="${defaultlabel}"/>
		</pre></p>
		<p>&nbsp;</p>
		
		<p><b>utils:localizeTitle [depreciee, voir utils:localizeProperty]</b> :<br/></p>
		<p>&nbsp;</p>
		
		<p><b>utils:navigationHref</b> :<br/>
		Permet d'extraire le path effectif du lien en fonction de la propri&eacute;t&eacute; default-file, de la pr&eacute;sence de fichier index, 
		et de la pr&eacute;sence de filiation de navigation (par ordre de priorit&eacute; descendante).<br/>
		<pre>
		&lt;utils:navigationHref file="${path}" recursive="true" />
		</pre></p>
		<p>&nbsp;</p>
		
		<p><b>utils:navigationLabel</b> :<br/>
		Permet de r&eacute;cup&eacute;rer le libell&eacute; &agrave; appliquer au lien, en fonction du filename, de la propri&eacute;t&eacute; Title, de la propri&eacute;t&eacute; NavText, 
		de la propri&eacute;t&eacute; NavTitle, et d'une valeur par d&eacute;faut.<br/>
		<pre>
		&lt;utils:navigationLabel file="${path}" custom="${link.value['Label']}" default="&lt;fmt:message key="mydefaultvalue" />" />
		</pre></p>
		<p>&nbsp;</p>
		
		<p><b>utils:orderListByLocalizedProperty [depuis 1.0.1.3]</b> :<br/>
		Tri une List de CmsResource ou CmsCategory selon leurs valeurs de propri&eacute;t&eacute; localis&eacute;e. Renvoie une map Map&lt;String,List&lt;CmsResource>> ou Map&lt;String,List&lt;CmsCategory>><br/>
		<pre>
		&lt;utils:orderListByLocalizedProperty var="${mapvar}" locale="${locale}" name="${propertyname}" list="${thelisttoorder}" /&gt;
		</pre></p>
		<p>&nbsp;</p>
		
		<p><u>Fonctions JSTL</u></p>
		
		<p><b>${utils:existsUri(pageContext,path)} [depuis 1.0.1.2]</b> :<br/>
		Cette fonction permet de tester l'existence d'un chemin, la variable pouvant contenir des param&egrave;tres (par exemple pour le scale des images) ou des ancres. 
		Permet d'&eacute;viter de faire un test sur la pr&eacute;sence des param&egrave;tres et des ancres.<br/>
		<pre>
		&lt;c:if test="${utils:existsUri(pageContext,imagePath)}">... 
		utils:existsUri(pageContext,xmlSiteConfig.value['BackgroundImage'].toString()) !!!! si c'est un VfsImage    <==== non, transformation .toString() appliqu&eacute; dans la function
		</pre></p>
		
		<p><b>${utils:fileContentAsString(pageContext,path)} [depuis 1.0.1.2]</b> :<br/>
		Cette fonction permet de r&eacute;cup&eacute;rer le contenu en String d'un fichier. La variable peut contenir des param&egrave;tres (par exemple pour le scale des images) ou des ancres. 
		Si un probl&egrave;me surgit ou si la ressource est un folder, renvoie null.<br/>
		<pre>
		${utils:fileContentAsString(pageContext,pointerPath)}     
		${utils:fileContentAsString(pageContext,page.toString())}     <==== non, transformation .toString() appliqu&eacute; dans la function             
		</pre>
		</p>
		
		<p><b>${utils:getHref(pageContext,path,bean)} [depuis 1.0.1.3]</b> :<br/>
		Cette fonction renvoie l'attribut href d'un lien, calcul&eacute; selon le type de ressource de la cible et la navigation. La variable peut contenir des param&egrave;tres (par exemple pour le scale des images) ou des ancres. 
		<br/><pre>
		&lt;c:set var="href" value="${utils:getHref(pageContext,Page,cmsAction)}" />        
		</pre>
		</p>
		
		<p><b>${utils:getRescaledImagePath(path, width, height)} [depuis 1.0.1.3]</b> :<br/>
		Cette fonction renvoie le chemin de l'image redimensionn&eacute;e selon les largeur et hauteur demand&eacute;s. 
		La variable peut contenir des param&egrave;tres (par exemple pour le scale des images) ou des ancres. 
		<br/><pre>
		&lt;c:if test="${cms.container.type == 'home33'}">&lt;c:set var="Image">${utils:getRescaledImagePath(Image, 233, '')}&lt;/c:set>&lt;/c:if>    
		</pre>
		</p>
		
		<p><b>${utils:getTarget(pageContext,path)} [depuis 1.0.1.3]</b> :<br/>
		Cette fonction renvoie "_blank" ou une cha&icirc;ne vide, selon le type de la ressource pass&eacute;e. La variable peut contenir des param&egrave;tres (par exemple pour le scale des images) ou des ancres. 
		<br/><pre>
		&lt;c:set var="Page">${item.value['AlignedBanner/Link']}&lt;/c:set>
    &lt;c:set var="target" value="${utils:getTarget(pageContext,Page)}" />           
		</pre>
		</p>
		
		<p><b>${utils:replaceTextarea(text)} [depuis 1.0.1.3]</b> :<br/>
		Cette fonction remplace les "\n" par des "&lt;br/>".<br/>
		<pre>
		&lt;div class="description" ${content.rdfa['Text']}>${utils:replaceTextarea(Text)}&lt;/div>      
		</pre>
		</p>
		
	</div>
</div>
<link href="../../resources/jquery/css/ui-ocms/jquery.ui.css" rel="stylesheet" type="text/css"></link>
<style type="text/css">
.ui-accordion-header{padding-left: 30px;}
.content{height:auto !important;}

</style>
<script type="text/javascript" src="../../resources/jquery/packed/jquery.js"></script>
<script type="text/javascript" src="../../resources/jquery/packed/jquery.ui.js"></script>
<script type="text/javascript">
$(function() {
    $( "#accordion" ).accordion();
});
</script>