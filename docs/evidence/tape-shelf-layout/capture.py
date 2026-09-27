from pathlib import Path
import subprocess,sys,xml.etree.ElementTree as E,re
serial='00252359V002047'
out=Path(__file__).parent
name=sys.argv[1]
def adb(*args):return subprocess.check_output(['adb','-s',serial,*args])
adb('shell','uiautomator','dump','/data/local/tmp/mixtape-review.xml')
xml=adb('exec-out','cat','/data/local/tmp/mixtape-review.xml')
root=E.fromstring(xml)
assert any(n.get('package')=='org.puzzleduck.mixtape' for n in root.iter('node')), 'Mixtape not visible; do not capture another app'
(out/(name+'.xml')).write_bytes(xml)
(out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))
for n in root.iter('node'):
 d=n.get('content-desc','')
 if d and n.get('bounds')!='[0,0][0,0]':print(d,n.get('bounds'))
