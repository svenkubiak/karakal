<#assign styleTag = "<script src=\"/api/v1/assets/" + app.appId + "/karakal.min.css\"></script>">
<#assign scriptTag = "<script src=\"/api/v1/assets/" + app.appId + "/karakal.min.js\"></script>">
<div class="content">
    <h3 class="subtitle is-6">Add the following stylesheets in your applications header:</h3>
<pre>
<code>${scriptTag}</code>
</pre>
    <h3 class="subtitle is-6">Add the following div element to your application body:</h3>
<pre>
<code>
&lt;div id=&quot;karakal-auth&quot;&gt;&lt;/div&gt;
</code>
</pre>
    <h3 class="subtitle is-6">Add the following javascript at your page footer:</h3>
<pre>
<code>${styleTag}</code>
</pre>
</div>