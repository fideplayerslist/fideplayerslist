package globals

object Globals
{
	val collected_keys=Array("birthday","country","rr","title","flag","st")

	val collected_titles=Array("GM","IM","WGM","FM","WIM","CM","WFM","WCM")

	val title_values=Map[String,Int]("GM"->100,"IM"->10,"WGM"->6,"FM"->5,"WIM"->4,"CM"->3,"WFM"->2,"WCM"->1)

	val keystat_fields=Array("ALL","R","CR","AVGR","MF","RMF","CRMF","AVGRMF","M","RM","CRM","AVGRM","F","PARF","RF","PARFR","CRF","AVGRF")

	val keystat_indices=(keystat_fields zip (1 to keystat_fields.length)).toMap

	val filters=Array("x","m","a","ma")

	val REFERENCE_YEAR=2015

	val rating_refinement=10
}