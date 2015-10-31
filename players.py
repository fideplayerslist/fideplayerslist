import xml.parsers.expat

cnt=0
state="idle"
current_key=""
current_value=""
fields=[]
outf=None

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
	if state=="player":
		if name=="player":
			state="idle"
			line="\t".join(fields)
			print(line,file=outf)
			fields=[]
			cnt+=1
			if((cnt%10000)==0):
				print(cnt,"players processed")
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
	p = xml.parsers.expat.ParserCreate()

	fh=open("players_list_xml.xml")

	outf=open("players.txt","w")
	
	p.StartElementHandler = start_element
	p.EndElementHandler = end_element
	p.CharacterDataHandler = char_data

	line=True

	while((cnt<100000) and line):

		line=fh.readline().rstrip()

		p.Parse(line)

	outf.close()

	fh.close()
	
process_xml();