from pathlib import Path
import math, random, wave

SR=44100
OUT=Path(__file__).resolve().parents[1]/'app'/'src'/'main'/'res'/'raw'
OUT.mkdir(parents=True, exist_ok=True)

def env(i,n,a=.03,r=.25):
    return max(0.0,min(1.0,i/max(1,int(n*a)),(n-i)/max(1,int(n*r))))

def write(name,seconds,fn):
    n=max(1,int(SR*seconds)); data=bytearray()
    for i in range(n):
        t=i/SR; x=max(-1.0,min(1.0,fn(t,i,n))); v=int(x*32767)
        data += int(v).to_bytes(2,'little',signed=True)
    with wave.open(str(OUT/name),'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(data)

random.seed(7)
write('tick.wav',.045,lambda t,i,n: env(i,n,.01,.55)*(.45*math.sin(2*math.pi*1450*t)+.12*random.uniform(-1,1)))
write('lock_click.wav',.11,lambda t,i,n: env(i,n,.01,.65)*(.55*math.sin(2*math.pi*620*t)+.25*math.sin(2*math.pi*1240*t)))
write('scrape.wav',.22,lambda t,i,n: env(i,n,.02,.18)*(.22*random.uniform(-1,1)+.12*math.sin(2*math.pi*(180+520*t)*t)))
write('alarm.wav',.85,lambda t,i,n: env(i,n,.01,.08)*.42*math.sin(2*math.pi*(720 if int(t*7)%2==0 else 980)*t))
write('vault_open.wav',.72,lambda t,i,n: env(i,n,.02,.35)*(.30*math.sin(2*math.pi*(130+40*t)*t)+.18*random.uniform(-1,1)*(1-t/.72)))
print('audio assets generated')
