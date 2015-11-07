import xml.parsers.expat
from tkinter import *
import time
import os
from os import walk

# global variables

REFERENCE_YEAR=2015

MIN_AGE=1
MAX_AGE=100

cnt=0
state="idle"
current_key=""
current_value=""
fields={}
outf=None
status_label=None
last_update=time.time()
key_counts={}
phase=0
sorted_keys=[]
collected=("country","birthday","flag","title")

filters=("","m","a","ma")
filters_nice={
	"":"among all players",
	"m":"among middle age players",
	"a":"among active players",
	"ma":"among middle age active players"
	}
	
charts=[
	"Select chart",
	"Female participation in function of age",
	"Rating difference in function of female participation",
	"Female average rating in the function of participation"
	]
	
other_stats=[
	"Other stats",
	"Rating for age"
	]

# end global variables

# chart format

CHART_HEIGHT=350
CHART_WIDTH=600
TITLE_Y_WIDTH=30
SCALE_Y_WIDTH=40
SCALE_X_HEIGHT=30
TITLE_X_HEIGHT=30
TITLE_HEIGHT=50
SCALE_FONT_SIZE=12
TITLE_FONT_SIZE=14
LEGEND_WIDTH=250
DASHFILL="#afaf00"
FONT="Times New Roman"
TITLE_FONT="Times New Roman"

# end chart format

root=Tk()

select_chart_combo_variable=StringVar(root)
select_chart_combo_variable.set("Select chart")

select_filter_variable=StringVar(root)
select_filter_variable.set("")

select_limit_variable=StringVar(root)
select_limit_variable.set("0")

select_other_stats_variable=StringVar(root)
select_other_stats_variable.set("Other stats")

root.geometry("+10+10")

PADX=5
PADY=5

chart_frame=Frame(root,padx=2*PADX,pady=2*PADY)

title_canvas=Canvas(chart_frame,width=CHART_WIDTH,height=TITLE_HEIGHT)
title_canvas.grid(row=0,column=2,padx=PADX,pady=PADY)
scale_x_canvas=Canvas(chart_frame,width=CHART_WIDTH,height=SCALE_X_HEIGHT)
scale_x_canvas.grid(row=2,column=2,padx=PADX,pady=PADY)
title_x_canvas=Canvas(chart_frame,width=CHART_WIDTH,height=TITLE_X_HEIGHT)
title_x_canvas.grid(row=3,column=2,padx=PADX,pady=PADY)
title_y_canvas=Canvas(chart_frame,width=TITLE_Y_WIDTH,height=CHART_HEIGHT)
title_y_canvas.grid(row=1,column=0,padx=PADX,pady=PADY)
scale_y_canvas=Canvas(chart_frame,width=SCALE_Y_WIDTH,height=CHART_HEIGHT)
scale_y_canvas.grid(row=1,column=1,padx=PADX,pady=PADY)
chart_canvas=Canvas(chart_frame,width=CHART_WIDTH,height=CHART_HEIGHT)
chart_canvas.grid(row=1,column=2,padx=PADX,pady=PADY)
legend_canvas=Canvas(chart_frame,width=LEGEND_WIDTH,height=CHART_HEIGHT)
legend_canvas.grid(row=1,column=3,padx=PADX,pady=PADY)

def update(message):
	global cnt
	global status_label
	global phase
	status_label.config(text="Phase "+str(phase)+" - "+message)
	status_label.update()

def start_element(name, attrs):
	global state
	global current_key
	global current_value
	if name=="player":
		state="player"
	elif state=="player":
		current_key=name
		
def end_element(name):
	global state
	global current_key
	global current_value
	global fields
	global cnt
	global outf
	global status_label
	global last_update
	global phase
	global key_counts
	global sorted_keys
	if state=="player":
		if name=="player":
			state="idle"
			if phase==2:
				line_list=map(lambda key:fields[key],sorted_keys)
				print("\t".join(line_list),file=outf)
			fields={}
			cnt+=1
			t=time.time()
			if (t-last_update)>=1:
				update("Processed records: "+str(cnt))
				last_update=t
		else:
			fields[current_key]=current_value
			if phase==1:
				value=current_value if not current_value=="" else "N/A"
				if current_key in key_counts:
					if value in key_counts[current_key]:
						key_counts[current_key][value]+=1
					else:
						key_counts[current_key][value]=1
				else:
					key_counts[current_key]={value:1}
			current_key=""
			current_value=""
			
def char_data(data):
	global state
	global current_key
	global current_value
	if state=="player" and not current_key=="":
		current_value=data

def process_xml_go():
	global cnt
	cnt=0
	p = xml.parsers.expat.ParserCreate()

	fh=open("players_list_xml.xml")
	
	p.StartElementHandler = start_element
	p.EndElementHandler = end_element
	p.CharacterDataHandler = char_data

	line=True

	while((cnt<1000000) and line):
		line=fh.readline().rstrip()
		if line:
			p.Parse(line)

	fh.close()
	update("Processing done, "+str(cnt)+" records processed")
	
def mkdir(path):
	if not os.path.exists(path):
		os.mkdir(path)
	
def process_xml():
	global phase
	global outf
	global sorted_keys
	phase=1
	process_xml_go();
		
	sorted_keys=list(key_counts.keys())
	sorted_keys.sort()
	
	mkdir("keycounts")
	
	for key in sorted_keys:
		print("creating keycount",key)
		keyf=open("keycounts/"+key+".txt","w")
		values=list(key_counts[key].keys())
		values.sort()
		for value in values:
			print(value+"\t"+str(key_counts[key][value]),file=keyf)
		keyf.close();
		
	phase=2
	outf=open("players.txt","w")
	print("\t".join(sorted_keys),file=outf)
	process_xml_go();
	outf.close()
	
def getkey(record,key):
	if not key in record:
		return "NA"
	return record[key] if not record[key]=="" else "NA"
		
addnocnt=0
def iterate_players_txt():
	global addnocnt
	global collected
	def addno(line):
		global addnocnt
		addnocnt+=1
		return str(addnocnt)+"\t"+line
	global cnt
	global last_update
	global phase
	fh=open("players.txt")
	headersline=fh.readline().rstrip()
	headers=headersline.split("\t")
	line=True
	cnt=0
	collections={}
	phase="Iterating players.txt"
	for key in collected:
		collections[key]={}
	while((cnt<1000000) and line):
		line=fh.readline().rstrip()
		if(line):
			fields=line.split("\t")
			cnt+=1
			record=dict(zip(headers,fields))
			for key in collected:
				subkey=getkey(record,key)
				if "rating" in record:
					touple=(int(record["rating"]),line) if not record["rating"]=="" else (0,line)
				else:
					touple=(0,line)
				if subkey in collections[key]:
					collections[key][subkey].append(touple)
				else:
					collections[key][subkey]=[touple]
			t=time.time()
			if (t-last_update)>=1:
				update("Processed records: "+str(cnt))
				last_update=t
	update("Iterating players.txt, scan ok, "+str(cnt)+" records processed")
	for key in collected:
		mkdir(key)
		for subkey in collections[key]:
			print("Generating "+key+" : "+subkey)
			outf=open(key+"/"+subkey+".txt","w")
			print("no\t"+"\t".join(headers),file=outf)
			l=list(map(lambda x:x[1],sorted(collections[key][subkey],key=lambda x:x[0],reverse=True)))
			addnocnt=0
			l=list(map(addno,l))
			print("\n".join(l),file=outf)
			outf.close()
			outf=open(key+"/"+subkey+".html","w")
			print("<table border=1><tr><td>no</td><td>"+"</td><td>".join(headers)+"</td></tr><tr>",file=outf)
			print("</tr><tr>".join(map(lambda x:("<td>"+"</td><td>".join(x.split("\t"))+"</td>"),l)),file=outf)
			print("</tr></table>",file=outf)
			outf.close()
	update("Iterating players.txt, done , "+str(cnt)+" records processed")
	
def average(counter,denominator):
	if denominator==0:
		return "NA"
	return "{0:.2f}".format(counter/denominator)
	
def create_stats_file(path,name,filter):
	print("Creating stats ",path,name,filter)
	fh=open(path+"/"+name+".txt")
	line=fh.readline()
	if not line:
		print("File empty, no stats created")
		return
	headers=line.rstrip().split("\t")
	linecnt=0
	
	stats={
		"ALL":0,
		"MF":0,
		"M":0,
		"F":0,
		"RALL":0,
		"RMF":0,
		"RM":0,
		"RF":0,
		"CRALL":0,
		"CRMF":0,
		"CRM":0,
		"CRF":0,
		"AVGRALL":0,
		"AVGRMF":0,
		"AVGRM":0,
		"AVGRF":0,
		"AVGRDIFF":0
		}
		
	while(line):
		line=fh.readline()
		if line:
			linecnt+=1
			fields=line.rstrip().split("\t")
			record=dict(zip(headers,fields))
			
			active="i" not in getkey(record,"flag")
			
			middleage=False
			birthday=getkey(record,"birthday")
			if not birthday=="NA":
				age=REFERENCE_YEAR-int(birthday)
				if age>=20 and age<=40:
					middleage=True
					
			cond=True
			
			if "a" in filter and not active:
				cond=False
				
			if "m" in filter and not middleage:
				cond=False
				
			if cond:
			
				rating_s=getkey(record,"rating")
				stats["ALL"]+=1
				sex=getkey(record,"sex")
				if sex in ("M","F"):
					stats["MF"]+=1
					stats[sex]+=1
				if not rating_s=="NA":
					rating=int(rating_s)
					stats["CRALL"]+=rating
					stats["RALL"]+=1
					if sex in ("M","F"):
						stats["CR"+sex]+=rating
						stats["R"+sex]+=1
						stats["CRMF"]+=rating
						stats["RMF"]+=1
					
	stats["AVGRALL"]=average(stats["CRALL"],stats["RALL"])
	stats["AVGRMF"]=average(stats["CRMF"],stats["RMF"])
	stats["AVGRM"]=average(stats["CRM"],stats["RM"])
	stats["AVGRF"]=average(stats["CRF"],stats["RF"])
	if (not stats["AVGRM"]=="NA") and (not stats["AVGRF"]=="NA"):
		stats["AVGRDIFF"]="{0:.2f}".format(float(stats["AVGRM"])-float(stats["AVGRF"]))
	else:
		stats["AVGRDIFF"]="NA"
	stats["PARF"]=average(100*stats["F"],stats["MF"])
	stats["PARFR"]=average(100*stats["RF"],stats["RMF"])
					
	outf=open(path+"/"+name+".stats"+filter+".txt","w")
	outfh=open(path+"/"+name+".stats"+filter+".html","w")
	print("<table border=1 cellpadding=5 cellspacing=5>",file=outfh)
	linecnt=0
	for key in sorted(stats.keys()):
		print(key+"\t"+str(stats[key]),file=outf)
		linecnt+=1
		print("<tr><td>"+str(linecnt)+"</td><td>"+key+"</td><td>"+str(stats[key])+"</tr>",file=outfh)
	print("</table>",file=outfh)
	outfh.close()
	outf.close()
			
	
def create_stats():
	global collected
	global status_label
	status_label.config(text="Creating stats")
	status_label.update()
	print("Creating stats")
	for key in collected:
		print("Listing",key)
		f=[]
		for(dirpath,dirnames,filenames) in walk(key):
			f.extend(filenames)
			break
		for name in f:
			parts=name.split(".")
			if len(parts)==2 and parts[1]=="txt":
				name=parts[0]
				
				for filter in filters:				
					create_stats_file(key,name,filter)
				
	status_label.config(text="Creating stats, done")
	status_label.update()
		
def startup():
	process_xml()
	iterate_players_txt()
	create_stats()
	create_stats_by_key()

def create_stats_by_key_file(key,name,filter):
	print("File",name,filter)
	record={}
	fh=open(key+"/"+name+".stats"+filter+".txt")
	line=True
	while(line):
		line=fh.readline()
		if line:
			fields=line.rstrip().split("\t")
			record[fields[0]]=fields[1]
	fh.close()
	return record
	
def create_stats_by_key():
	global collected
	
	for filter in filters:
	
		status_label.config(text="Creating stats by key with filter "+filter)
		status_label.update()
		
		mkdir("keystats")
		
		for key in collected:
			print("Creating stats for key",key)
			lines=[]
			f=[]
			for(dirpath,dirnames,filenames) in walk(key):
				f.extend(filenames)
				break
			for name in f:
				parts=name.split(".")
				if len(parts)==3 and parts[1]=="stats" and parts[2]=="txt":
					name=parts[0]
					
					################
					record=create_stats_by_key_file(key,name,filter)
					################
					
					lines.append((name,record))
					
			if key=="country":
				lines.sort(key=lambda x:0.0 if x[1]["PARFR"]=="NA" else float(x[1]["PARFR"]),reverse=True)
			elif key=="birthday":
				lines.sort(key=lambda x:0 if x[0]=="NA" else int(x[0]),reverse=True)
				
			stats_headere=[key]
			sorted_keys=sorted(record.keys())
			stats_headere.extend(sorted_keys)
			
			linese=[stats_headere]
			for line in lines:
				array=[line[0]]
				for rkey in sorted_keys:
					array.append(line[1][rkey])
				linese.append(array)
			
			outf=open("keystats/"+key+filter+".txt","w")
			outfh=open("keystats/"+key+filter+".html","w")
			print("<table border=1>",file=outfh)
			for line in linese:
				print("\t".join(line),file=outf)
				print("<tr><td>"+"</td><td>".join(line)+"</td></tr>",file=outfh)
			print("</table>",file=outfh)
			outfh.close()
			outf.close()
	
	status_label.config(text="Creating stats by key, done")
	status_label.update()
	
def parse_txt(path):
	fh=open(path)
	line=fh.readline()
	headers=line.rstrip().split("\t")
	records=[]
	while(line):
		line=fh.readline()
		if(line):
			fields=line.rstrip().split("\t")
			record=dict(zip(headers,fields))
			records.append(record)
	return records
	
def extract(records,key,value,func,cond):
	global MIN_X,MAX_X
	keyvaluepairs=[]
	for record in records:
		if not record[key]=="NA" and not record[value]=="NA":
			x=func(record[key])
			y=float(record[value])
			if x>=MIN_X and x<=MAX_X:
				keyvaluepair=(x,y)
				if(cond(record)):
					keyvaluepairs.append(keyvaluepair)
	return keyvaluepairs
	
def draw_rect(x,y,color,size):
	cx=(x-MIN_X)*FACTOR_X
	cy=CHART_HEIGHT-(y-MIN_Y)*FACTOR_Y
	chart_canvas.create_rectangle(cx+5,cy-size,cx+5+2*size,cy+size,fill=color)

def calc_linear(XYS):
	Sx=0
	Sy=0
	Sxx=0
	Sxy=0
	Syy=0
	n=0
	for xy in XYS:
		x=xy[0]
		y=xy[1]
		Sx+=x
		Sy+=y
		Sxx+=x*x
		Sxy+=x*y
		Syy+=y*y
		n+=1
		
	Beta=(n*Sxy-Sx*Sy)/((n*Sxx)-(Sx*Sx))
	Alpha=(Sy/n)-(Beta*Sx/n)
	
	return (Alpha,Beta)
	
def draw_chart():
	global LXYS,MIN_X,MAX_X,MIN_Y,MAX_Y,STEP_X,STEP_Y,TITLE,SUBTITLE,TITLE_X,TITLE_Y,LEGEND,RANGE_X,RANGE_Y,FACTOR_X,FACTOR_Y
	
	title_canvas.delete("all")
	title_x_canvas.delete("all")
	scale_x_canvas.delete("all")
	title_y_canvas.delete("all")
	scale_y_canvas.delete("all")
	chart_canvas.delete("all")
	legend_canvas.delete("all")
	
	RANGE_X=MAX_X-MIN_X
	FACTOR_X=CHART_WIDTH/RANGE_X
	RANGE_Y=MAX_Y-MIN_Y
	FACTOR_Y=CHART_HEIGHT/RANGE_Y
	x=MIN_X
	chart_canvas.create_line(0,CHART_HEIGHT-1,CHART_WIDTH-10,CHART_HEIGHT-1)
	chart_canvas.create_line(2,10,2,CHART_HEIGHT-1)
	
	while(x<MAX_X):
		cx=FACTOR_X*(x-MIN_X)
		scale_x_canvas.create_text(cx+5,5,text=str(x),anchor="nw",font=(FONT,SCALE_FONT_SIZE))
		chart_canvas.create_line(cx,15,cx,CHART_HEIGHT-1,dash=[5,5],fill=DASHFILL)
		x+=STEP_X
		
	y=MIN_Y
	
	while(y<MAX_Y):
		cy=FACTOR_Y*(y-MIN_Y)
		scale_y_canvas.create_text(5,CHART_HEIGHT-(cy+15),text=str(y),anchor="nw",font=(FONT,SCALE_FONT_SIZE))
		chart_canvas.create_line(0,cy,CHART_WIDTH-15,cy,dash=[5,5],fill=DASHFILL)
		y+=STEP_Y
		
	title_canvas.create_text((CHART_WIDTH-len(TITLE)*int(TITLE_FONT_SIZE*0.8))/2,2,text=TITLE+"\n "+SUBTITLE,anchor="nw",font=(TITLE_FONT,int(TITLE_FONT_SIZE*1.2)))
	title_x_canvas.create_text((CHART_WIDTH-len(TITLE_X)*TITLE_FONT_SIZE)/2,2,text=TITLE_X,anchor="nw",font=(TITLE_FONT,TITLE_FONT_SIZE))
	title_y_canvas.create_text(2,(CHART_HEIGHT-len(TITLE_Y)*TITLE_FONT_SIZE*1.3)/2,text="\n".join(TITLE_Y),anchor="nw",font=(TITLE_FONT,TITLE_FONT_SIZE))
	
	legend_cnt=0
	legends=LEGEND.split("\t")
	legendcolors=[]
	legendtexts=[]
	dolinears=[]
	for legend in legends:
		parts=legend.split(":")
		legendtext=parts[0]
		legendcolor=parts[1]
		dolinear_part=parts[2]
		dolinear=True if dolinear_part=="t" else False
		legendcolors.append(legendcolor)
		legendtexts.append(legendtext)
		dolinears.append(dolinear)
		cy=legend_cnt*40+5
		legend_canvas.create_rectangle(5,cy,15,cy+10,fill=legendcolor)
		legend_canvas.create_text(30,cy-5,text=legendtext,anchor="nw",font=(TITLE_FONT,TITLE_FONT_SIZE))
		legend_cnt+=1
	
	linear_cnt=0
	for legend in legends:
		if dolinears[linear_cnt]:
			cy=legend_cnt*40+5
			legend_canvas.create_rectangle(5,cy+3,30,cy+7,fill=legendcolors[linear_cnt])
			legend_canvas.create_text(40,cy-5,text=legendtexts[linear_cnt]+" linear",anchor="nw",font=(TITLE_FONT,TITLE_FONT_SIZE))
		linear_cnt+=1
		legend_cnt+=1
		
	xysi=0
	for XYS in LXYS:
		print("series",xysi,"size",len(XYS))
		color=legendcolors[xysi]
		for xy in XYS:
			x=xy[0]
			y=xy[1]
			if x>=MIN_X and x<=MAX_X:
				draw_rect(x,y,color,5)
		if dolinears[xysi]:
			alphabeta=calc_linear(XYS)
			Alpha=alphabeta[0]
			Beta=alphabeta[1]
			cx0=0
			cx1=CHART_WIDTH
			cy0=CHART_HEIGHT-(Alpha+MIN_X*Beta-MIN_Y)*FACTOR_Y
			cy1=CHART_HEIGHT-(Alpha+MAX_X*Beta-MIN_Y)*FACTOR_Y
			chart_canvas.create_line(cx0,cy0,cx1,cy1,width=5,fill=color)
		xysi+=1
		
def draw_par_chart():
	global LXYS,MIN_X,MAX_X,MIN_Y,MAX_Y,STEP_X,STEP_Y,TITLE,SUBTITLE,TITLE_X,TITLE_Y,LEGEND,RANGE_X,RANGE_Y,FACTOR_X,FACTOR_Y
		
	MIN_X=0
	MAX_X=100
	STEP_X=10
	MIN_Y=0
	MAX_Y=60
	STEP_Y=10
	TITLE="Female participation in the function of age"
	TITLE_X="Age"
	TITLE_Y="Female %"
	LEGEND="Among all players:#ff0000:t\tAmong rated players:#0000ff:t"
	
	filter=select_filter_variable.get()
	
	SUBTITLE=filters_nice[filter]
	
	records=parse_txt("keystats/birthday"+filter+".txt")
	
	LXYS=(
		extract(records,"birthday","PARF",lambda x:REFERENCE_YEAR-float(x),lambda x:True),
		extract(records,"birthday","PARFR",lambda x:REFERENCE_YEAR-float(x),lambda x:True)
		)
	
	draw_chart()
	
def draw_rpar_chart():
	global LXYS,MIN_X,MAX_X,MIN_Y,MAX_Y,STEP_X,STEP_Y,TITLE,SUBTITLE,TITLE_X,TITLE_Y,LEGEND,RANGE_X,RANGE_Y,FACTOR_X,FACTOR_Y
		
	MIN_X=0
	MAX_X=45
	STEP_X=5
	MIN_Y=-100
	MAX_Y=500
	STEP_Y=50
	TITLE="Rating difference in the function of participation"
	TITLE_X="Participation %"
	TITLE_Y="Rating diff"
	LEGEND="Rating difference:#0000ff:t"
	
	filter=select_filter_variable.get()
	
	records=parse_txt("keystats/country"+filter+".txt")
	
	limit=int(select_limit_variable.get())
	
	SUBTITLE=filters_nice[filter]+" , more than "+str(limit)+" rated players"
		
	LXYS=[
		extract(records,"PARFR","AVGRDIFF",lambda x:float(x),lambda x:False if x["RMF"]=="NA" else int(x["RMF"])>limit)
		]
	
	draw_chart()
	
def draw_apar_chart():
	global LXYS,MIN_X,MAX_X,MIN_Y,MAX_Y,STEP_X,STEP_Y,TITLE,SUBTITLE,TITLE_X,TITLE_Y,LEGEND,RANGE_X,RANGE_Y,FACTOR_X,FACTOR_Y
		
	MIN_X=0
	MAX_X=45
	STEP_X=5
	MIN_Y=1000
	MAX_Y=2400
	STEP_Y=200
	TITLE="Female average rating in the function of participation"
	TITLE_X="Participation %"
	TITLE_Y="Rating"
	LEGEND="Female average rating:#0000ff:t"
	
	filter=select_filter_variable.get()
	
	records=parse_txt("keystats/country"+filter+".txt")
	
	limit=int(select_limit_variable.get())
	
	SUBTITLE=filters_nice[filter]+" , more than "+str(limit)+" rated players"
		
	LXYS=[
		extract(records,"PARFR","AVGRF",lambda x:float(x),lambda x:False if x["RMF"]=="NA" else int(x["RMF"])>limit)
		]
	
	draw_chart()
	
def select_chart_combo_selected(arg):
	arg=select_chart_combo_variable.get()
	if arg=="Female participation in function of age":
		draw_par_chart()
	if arg=="Rating difference in function of female participation":
		draw_rpar_chart()
	if arg=="Female average rating in the function of participation":
		draw_apar_chart()
		
def get_age_stats():
	records=parse_txt("keystats/birthday.txt")
	age_stats={}
	for record in records:
		if not record["birthday"]=="NA":
			birthday=int(record["birthday"])
			age=REFERENCE_YEAR-birthday
			if age>=MIN_AGE and age<=MAX_AGE:
				age_stats[age]=record
	return age_stats
	
def htmlify(path,name):
	fh=open(path+"/"+name+".txt")
	line=fh.readline()
	outf=open(path+"/"+name+".html","w")
	print("<table border=1>",file=outf)
	while(line):
		print("<tr><td>"+"</td><td>".join(line.rstrip().split("\t"))+"</td></tr>",file=outf)
		line=fh.readline()
	print("</table>",file=outf)		
	outf.close()
	
def rating_for_age():
	age_stats=get_age_stats()
	players=[]
	for age in range(MIN_AGE,MAX_AGE+1):
		birthday=REFERENCE_YEAR-age
		path="birthday/"+str(birthday)+".txt"
		if(os.path.isfile(path)):
			print("proessing age ",age)
			records=parse_txt(path)
			EXPRM_str=getkey(age_stats[age],"AVGRM")
			EXPRF_str=getkey(age_stats[age],"AVGRF")
			for record in records:
				if "rating" in record and "flag" in record:
					if not record["rating"]=="NA" and not record["rating"]=="" and "sex" in record and not "i" in record["flag"]:
						sex=record["sex"]
						if sex in ("M","F"):
							rating=int(record["rating"])
							EXPR_str=EXPRM_str if sex=="M" else EXPRF_str
							EXPR=float(EXPR_str)
							rating_diff=rating-EXPR
							record["EXPR"]=EXPR
							record["age"]=age
							record["rating_diff"]="{0:.2f}".format(rating_diff)
							players.append((rating_diff,record))
	players.sort(key=lambda x:x[0],reverse=True)
	lines=["rank\tname\tcountry\tage\tgender\texpected rating\tactual rating\trating surplus"]
	for i in range(0,100):
		record=players[i][1]
		line=str(i+1)+"\t"+record["name"]+"\t"+record["country"]+"\t"+str(record["age"])+"\t"+record["sex"]+"\t"+str(record["EXPR"])+"\t"+record["rating"]+"\t"+record["rating_diff"]
		lines.append(line)
	outf=open("otherstats/ratingforage.txt","w")
	print("\n".join(lines),file=outf)
	outf.close()
	htmlify("otherstats","ratingforage")
				
		
def select_other_stats_combo_selected(arg):
	mkdir("otherstats")
	if arg=="Rating for age":
		rating_for_age()
		
def do_movie():
	for chart in charts[2:]:
		select_chart_combo_variable.set(chart)
		for filter in filters:
			select_filter_variable.set(filter)
			for limit in limits:
				select_limit_variable.set(limit)
				select_chart_combo_selected("")
				root.update()
				t=time.time()
				while((time.time()-t)<1):
					t=t
				
# mainloop

status_label=Label(root)
status_label.pack(padx=2*PADX,pady=2*PADY)

buttons_frame=Frame(root)
buttons_frame.pack()

BUTTON_WIDTH=20

startup_button = Button(buttons_frame, text='STARTUP' , width=BUTTON_WIDTH, command=startup)
startup_button.grid(row=0,column=0, padx=PADX)

process_xml_button = Button(buttons_frame, text='Process XML', width=BUTTON_WIDTH, command=process_xml)
process_xml_button.grid(row=0,column=1, padx=PADX)

iterate_txt_button = Button(buttons_frame, text='Inerate players.txt', width=BUTTON_WIDTH, command=iterate_players_txt)
iterate_txt_button.grid(row=0,column=2, padx=PADX)

create_stats_button = Button(buttons_frame, text='Create stats', width=BUTTON_WIDTH, command=create_stats)
create_stats_button.grid(row=0,column=3, padx=PADX)

create_stats_by_key_button = Button(buttons_frame, text='Create stats by key', width=BUTTON_WIDTH, command=create_stats_by_key)
create_stats_by_key_button.grid(row=0,column=4, padx=PADX)

movie_button = Button(buttons_frame, text='Movie', width=BUTTON_WIDTH, command=do_movie)
movie_button.grid(row=0,column=5, padx=PADX)

options_frame=Frame(root,padx=2*PADX,pady=2*PADY)
options_frame.pack()
	
select_chart_combo = OptionMenu(options_frame,select_chart_combo_variable,*charts,command=select_chart_combo_selected)
select_chart_combo.grid(row=0,column=0)

select_filter_combo = OptionMenu(options_frame,select_filter_variable,*filters,command=select_chart_combo_selected)
select_filter_combo.grid(row=0,column=1)

limits=["0","10","20","30","40","50","100","150","200","250","300","350","400","450","500","600","700","800","900","1000"]

select_limit_combo = OptionMenu(options_frame,select_limit_variable,*limits,command=select_chart_combo_selected)
select_limit_combo.grid(row=0,column=2)

select_other_stats_combo = OptionMenu(options_frame,select_other_stats_variable,*other_stats,command=select_other_stats_combo_selected)
select_other_stats_combo.grid(row=0,column=3)

chart_frame.pack()

root.mainloop()