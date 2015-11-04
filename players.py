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
	
# mainloop
	
root=Tk()

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

root.mainloop()