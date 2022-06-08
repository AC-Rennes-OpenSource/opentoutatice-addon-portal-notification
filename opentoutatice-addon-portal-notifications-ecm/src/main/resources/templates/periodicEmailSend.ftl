<!DOCTYPE html>
<html>
<head>
<!-- logos are from Toutatice source : http://www.toutatice.fr/portail/cms/seria/intranet-rectorat/Actualit%C3%A9s%20Rectorat/evolution-de-la-charte.7514137724919656 -->
<style>
table {
    font-family: arial, sans-serif;
    border-collapse: collapse;
    width: 100%;
}

th {
	background-color: #b4e4b4;
}

td:FIRST-CHILD {
	width: 20%;
}

td, th {
    border: 1px solid black;
    text-align: left;
    padding: 8px;
}

tr:nth-child(even) {
    background-color: #d9f2d9;
}

#left-vertical-banner {
	width:18%;
	float:left;
	margin-right:25px
}

#right-top-banner {
	width:75%;
	float:left
}

#right-top-banner-marianne {
	margin-bottom: 70px;
}

#img-logo {
	max-width: 100%;
	max-height: 100%;	
}

#img-marianne {
	width: 15%;
	margin-left:30%;
	padding-top:30px
}

</style>
</head>
<body>
<div>
	<div id="left-vertical-banner">
		<img id="img-logo" src="https://www.ac-rennes.fr/sites/ac_rennes/files/site_logo/2021-02/AcadRennes20-logoC-SITE130pp.jpg">
	</div>
	<div id="right-top-banner">
		<div id="right-top-banner-marianne">

		</div>
		<div id="right-bottom-mail-content">
			<h3>Objet : ${mailSubject}</h3>
			<br/>
				
			Voici les événements qui ont eu lieu sur votre espace Module ${lastDateStr}
			<br/><br/>

			${notifiedDocs}

			<br/>
			
			
			
			La plateforme d'assistance informatique AMIGO est à votre disposition pour tout renseignement complémentaire :<br/>
			<ul>
			<li>Formulaire en ligne : <a href="http://assistance.ac-rennes.fr" target="_blank">http://assistance.ac-rennes.fr</a></li>
			<li>Adresse électronique : <a href="mailto:assistance@ac-rennes.fr">assistance@ac-rennes.fr</a></li>
			</ul>
			<br/>
			Cordialement,<br/>
			Le service informatique académique<br/><br/>
		</div>
	</div>
</div>
</body>
</html>