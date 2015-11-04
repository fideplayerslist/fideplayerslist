import xml.parsers.expat
from tkinter import *
import time
import os
from os import walk

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
collected=("country","birthday","flag")

CHART_HEIGHT=300
CHART_WIDTH=600
TITLE_Y_WIDTH=30
SCALE_Y_WIDTH=60
SCALE_X_HEIGHT=30
TITLE_X_HEIGHT=30
TITLE_HEIGHT=50
SCALE_FONT_SIZE=12
TITLE_FONT_SIZE=14
LEGEND_WIDTH=250

FONT="Times New Roman"
TITLE_FONT="Courier New"

MIN_X=0
MAX_X=100
STEP_X=20
MIN_Y=0
MAX_Y=100
STEP_Y=20
TITLE_X="Title X"
TITLE_Y="Title Y"
TITLE="Chart Title"
SUBTITLE="Subtitle"

root=Tk()

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
	
def create_stats_file(path,name):
	print("Creating stats ",path,name)
	fh=open(path+"/"+name+".txt")
	line=fh.readline()
	if not line:
		print("File empty, no stats created")
		return
	headers=line.rstrip().split("\t")
	linecnt=0
	stats={"ALL":0,"MF":0,"M":0,"F":0,"RALL":0,"RMF":0,"RM":0,"RF":0,"CRALL":0,"CRMF":0,"CRM":0,"CRF":0,"AVGRALL":0,"AVGRMF":0,"AVGRM":0,"AVGRF":0}
	while(line):
		line=fh.readline()
		if line:
			linecnt+=1
			fields=line.rstrip().split("\t")
			record=dict(zip(headers,fields))
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
	stats["PARF"]=average(100*stats["F"],stats["MF"])
	stats["PARFR"]=average(100*stats["RF"],stats["RMF"])
					
	outf=open(path+"/"+name+".stats.txt","w")
	outfh=open(path+"/"+name+".stats.html","w")
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
				create_stats_file(key,name)
				
	status_label.config(text="Creating stats, done")
	status_label.update()
		
def startup():
	process_xml()
	iterate_players_txt()
	create_stats()
	create_stats_by_key()

stats_header=[]
def create_stats_by_key_file(key,name):
	global stats_header
	print("File",name)
	array=[]
	fh=open(key+"/"+name+".stats.txt")
	stats_header=[]
	line=True
	while(line):
		line=fh.readline()
		if line:
			fields=line.rstrip().split("\t")
			array.append(fields[1])
			stats_header.append(fields[0])
	fh.close()
	return array
	
def create_stats_by_key():
	global collected
	global stats_header
	status_label.config(text="Creating stats by key")
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
				array=create_stats_by_key_file(key,name)
				arraye=[name]
				arraye.extend(array)
				lines.append(arraye)
				
		if key=="country":
			lines.sort(key=lambda x:0.0 if x[14]=="NA" else float(x[14]),reverse=True)
		elif key=="birthday":
			lines.sort(key=lambda x:0 if x[0]=="NA" else int(x[0]),reverse=True)
			
		stats_headere=[key]
		stats_headere.extend(stats_header)
		
		linese=[stats_headere]
		linese.extend(lines)
		
		outf=open("keystats/"+key+".txt","w")
		outfh=open("keystats/"+key+".html","w")
		print("<table border=1>",file=outfh)
		for line in linese:
			print("\t".join(line),file=outf)
			print("<tr><td>"+"</td><td>".join(line)+"</td></tr>",file=outfh)
		print("</table>",file=outfh)
		outfh.close()
		outf.close()
	
	status_label.config(text="Creating stats by key, done")
	status_label.update()
	
def draw_chart():
	RANGE_X=MAX_X-MIN_X
	FACTOR_X=CHART_WIDTH/RANGE_X
	RANGE_Y=MAX_Y-MIN_Y
	FACTOR_Y=CHART_HEIGHT/RANGE_Y
	x=MIN_X
	chart_canvas.create_line(0,CHART_HEIGHT-1,CHART_WIDTH-10,CHART_HEIGHT-1)
	chart_canvas.create_line(2,10,2,CHART_HEIGHT-1)
	while(x<MAX_X):
		cx=FACTOR_X*x
		scale_x_canvas.create_text(cx+5,5,text=str(x),anchor="nw",font=(FONT,SCALE_FONT_SIZE))
		x+=STEP_X
	y=MIN_Y
	while(y<MAX_Y):
		cy=FACTOR_Y*y
		scale_y_canvas.create_text(5,CHART_HEIGHT-(cy+5),text=str(y),anchor="sw",font=(FONT,SCALE_FONT_SIZE))
		y+=STEP_Y
	title_canvas.create_text((CHART_WIDTH-len(TITLE)*TITLE_FONT_SIZE)/2,2,text=TITLE+"\n "+SUBTITLE,anchor="nw",font=(TITLE_FONT,TITLE_FONT_SIZE))
	title_x_canvas.create_text((CHART_WIDTH-len(TITLE_X)*TITLE_FONT_SIZE)/2,2,text=TITLE_X,anchor="nw",font=(TITLE_FONT,TITLE_FONT_SIZE))
	title_y_canvas.create_text(2,(CHART_HEIGHT-len(TITLE_Y)*TITLE_FONT_SIZE)/2,text="\n".join(TITLE_Y),anchor="nw",font=(TITLE_FONT,TITLE_FONT_SIZE))
	
# mainloop

status_label=Label(root)
status_label.pack()

startup_button = Button(root, text='STARTUP', width=100, command=startup)
startup_button.pack()

process_xml_button = Button(root, text='Process XML', width=100, command=process_xml)
process_xml_button.pack()

iterate_txt_button = Button(root, text='Inerate players.txt', width=100, command=iterate_players_txt)
iterate_txt_button.pack()

create_stats_button = Button(root, text='Create stats', width=100, command=create_stats)
create_stats_button.pack()

create_stats_by_key_button = Button(root, text='Create stats by key', width=100, command=create_stats_by_key)
create_stats_by_key_button.pack()

draw_chart_button = Button(root, text='Draw chart', width=100, command=draw_chart)
draw_chart_button.pack()

chart_frame.pack()

root.mainloop()