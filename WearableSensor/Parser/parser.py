import time
import json



def writeToFile(sensorName, data):
	writeFile = open(sensorName + ".csv", 'a')
	writeFile.write(data)
	writeFile.close()
	


def convertEntryToJSON(packetEntry):
	#print(packetEntry)
	jsonEntry = json.loads(packetEntry)
	sensorJSON = jsonEntry  #IDK how to set this to a null
	sensorName = ""
	
	#ACCELEROMETER
	if "ACCELEROMETER" in jsonEntry:
		sensorName = "ACCELEROMETER"
		sensorJSON = jsonEntry["ACCELEROMETER"]
		#print(jsonEntry["ACCELEROMETER"])
	#GYROSCOPE
	elif "GYROSCOPE" in jsonEntry:
		sensorName = "GYROSCOPE"
		sensorJSON = jsonEntry["GYROSCOPE"]
		#print(jsonEntry["GYROSCOPE"])
	#GRAVITY
	elif "GRAVITY" in jsonEntry:
		sensorName = "GRAVITY"
		sensorJSON = jsonEntry["GRAVITY"]
		#print(jsonEntry["GRAVITY"])
	#LINEAR_ACCELEROMETER
	elif "LINEAR_ACCELEROMETER" in jsonEntry:
		sensorName = "LINEAR_ACCELEROMETER"
		sensorJSON = jsonEntry["LINEAR_ACCELEROMETER"]
		#print(jsonEntry["LINEAR_ACCELEROMETER"])
		
	entryTimestamp = sensorJSON["timestamp"]
	entryValues = sensorJSON["values"]
	output = sensorName + "," + str(entryTimestamp) + "," + str(entryValues[0]) + "," + str(entryValues[1]) + "," + str(entryValues[2])
	#print(output)
	writeToFile(sensorName, output)

def separatePackets(line):

	#Separate each packet into individual entries
	packetEntries = line.strip("\n")
	packetEntries = packetEntries[:-1]
	packetEntries = packetEntries.split('^')

	for packetEntry in packetEntries:
		if len(packetEntry) < 1:
			continue
		#This gives us the number of samples in this packet
		if(packetEntry[0] == '['):
			#Grab the sample counts of each packet
			indexEndOfSampleCount = packetEntry.find(']')
			#Grab everything between []
			sampleVals = packetEntry[1:indexEndOfSampleCount]
			sampleVals = sampleVals.split(',')
			sampleVals = [int(x) for x in sampleVals]
			#print(packetEntry[indexEndOfSampleCount+1:])
			convertEntryToJSON(packetEntry[indexEndOfSampleCount+1:])
		#This is just an ordinary packet entry
		else:
			#print(packetEntry)
			convertEntryToJSON(packetEntry)
	



rawFile = open("RAW.txt", 'r')
line = rawFile.readline()

# use the read line to read further.
# If the file is not empty keep reading one line
# at a time, till the file is empty
while line:
	#print(line)
	separatePackets(line)
	#time.sleep(2)
	line = rawFile.readline()
	#print("NEWLINE>>>>>>")
rawFile.close()