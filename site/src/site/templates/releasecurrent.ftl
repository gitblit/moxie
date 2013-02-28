<#include "macros.ftl" >

<!-- CURRENT RELEASE -->
<@LogMacro title="Current Release" log=release version=project.releaseVersion date=project.releaseDate description="this is the current stable release" />

<!-- NEXT RELEASE -->
<@LogMacro title="Next Release" log=snapshot version=project.version date="PENDING" description="these changes are queued for an upcoming release" />

<div>
	<ul class="pager">
		<li class="next"><a href="releases.html">All Releases &rarr;</a></li>
	</ul>
</div>
