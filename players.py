import xml.parsers.expat
from tkinter import *
import time

cnt=0
state="idle"
current_key=""
current_value=""
fields=[]
outf=None
status_label=None
last_update=time.time()

def update(message):
	global cnt
	global status_label
	status_label.config(text=message)
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
	if state=="player":
		if name=="player":
			state="idle"
			line="\t".join(fields)
			print(line,file=outf)
			fields=[]
			cnt+=1
			t=time.time()
			if (t-last_update)>=1:
				update("Processed records: "+str(cnt))
				last_update=t
		else:
			fields.append(current_value)
			current_key=""
			current_value=""
			
def char_data(data):
	global state
	global current_key
	global current_value
	if state=="player" and not current_key=="":
		current_value=data

def process_xml():
	global outf
	global cnt
	p = xml.parsers.expat.ParserCreate()

	fh=open("players_list_xml.xml")

	outf=open("players.txt","w")
	
	p.StartElementHandler = start_element
	p.EndElementHandler = end_element
	p.CharacterDataHandler = char_data

	line=True

	while((cnt<1000000) and line):

		line=fh.readline().rstrip()

		p.Parse(line)

	outf.close()

	fh.close()
	update("Processing done, "+str(cnt)+" records processed")
	
# mainloop
	
root=Tk()

status_label=Label(root)
status_label.pack()

process_xml_button = Button(root, text='Process XML', width=25, command=process_xml)
process_xml_button.pack()

root.mainloop()