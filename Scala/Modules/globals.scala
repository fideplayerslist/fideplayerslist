package globals

object Globals
{
	val collected_keys=Array("birthday","country","rr","title","flag")

	val keystat_fields=Array("ALL","R","CR","AVGR","MF","RMF","CRMF","AVGRMF","M","RM","CRM","AVGRM","F","PARF","RF","PARFR","CRF","AVGRF")

	val keystat_indices=(keystat_fields zip (1 to keystat_fields.length)).toMap

	val filters=Array("x","m","a","ma")

	val REFERENCE_YEAR=2015

	val rating_refinement=10
}