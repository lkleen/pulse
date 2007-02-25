<select<#rt/>
 name="${parameters.name?default("")?html}"<#rt/>
<#if parameters.size?exists>
 size="${parameters.size?html}"<#rt/>
</#if>
<#if parameters.disabled?default(false)>
 disabled="disabled"<#rt/>
</#if>
<#if parameters.tabindex?exists>
 tabindex="${parameters.tabindex?html}"<#rt/>
</#if>
<#if parameters.id?exists>
 id="${parameters.id?html}"<#rt/>
</#if>
<#if parameters.multiple?exists>
 multiple="multiple"<#rt/>
</#if>
>
<#if parameters.headerKey?exists && parameters.headerValue?exists>
    <option value="${parameters.headerKey?html}">${parameters.headerValue?html}</option>
</#if>
<#if parameters.emptyOption?default(false)>
    <option value=""></option>
</#if>
<#list parameters.list as item>
    <#if parameters.listKey?exists>
        <#assign itemKey = item[parameters.listKey]/>
    <#else>
        <#assign itemKey = item/>
    </#if>
    <#if parameters.listValue?exists>
        <#assign itemValue = item[parameters.listValue]/>
    <#else>
        <#assign itemValue = item/>
    </#if>
    <option value="${itemKey?html}"<#rt/>
        <#if parameters.value?exists && parameters.value == itemKey>
 selected="selected"<#rt/>
        </#if>
    >${itemValue?html}</option><#lt/>
</#list>
</select>