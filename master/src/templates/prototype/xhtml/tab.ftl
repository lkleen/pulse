<table>
<#assign tablewidth = table.columns?size + table.actions?size/>
    <tr>
        <th class="content" colspan="${tablewidth}">${"table.header.label"?i18n}</th>
    </tr>
    <tr>
<#list table.columns as column>
<#assign header>${column.name}.label</#assign>
        <th class="content" colspan="1">${header?i18n}</th>
</#list>
<#if table.actions?size &gt; 0>
        <th class="content" colspan="${table.actions?size}">${"actions.label"?i18n}</th>
</#if>
    </tr>
<#if data?size &gt; 0>
<#list data as item>
        <tr>
<#list table.columns as column>
            <td class="content">${item[column.name]}</td>
</#list>
<#list table.actions as action>
<#assign actionlabel>${action}.label</#assign>
<#if action == "edit">
            <td class="content"><a href="/config/${item.configurationPath}">${"edit.label"?i18n}</a></td>
<#elseif action == "delete">
            <td class="content"><a href="/config/${item.configurationPath}?${action}=confirm">${"delete.label"?i18n}</a>  </td>
<#else>
            <td class="content"><a href="/config/${item.configurationPath}?${action}">${actionlabel?i18n}</a>  </td>
</#if>
</#list>
        </tr>
</#list>
<#else>
    <tr>
        <td class="content" colspan="${tablewidth}">${no.data.available?i18n}</td>
    </tr>
</#if>
    <tr>
        <td class="content" colspan="${tablewidth}"><a href="/config/${path}?wizard">${"add.label"?i18n}</a></td>
    </tr>
</table>