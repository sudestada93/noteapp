"""Mini motor de grabado: sombreado con líneas paralelas recortadas a formas."""
import math, random
import numpy as np
from PIL import Image, ImageDraw
from shapely.geometry import Polygon, LineString, MultiLineString, GeometryCollection, Point
from shapely import affinity

SS = 3  # supersampling

def P(pts):
    return Polygon(pts).buffer(0)

def ellipse(cx, cy, rx, ry, n=90, rot=0):
    pts = [(cx + rx*math.cos(2*math.pi*i/n), cy + ry*math.sin(2*math.pi*i/n)) for i in range(n)]
    poly = Polygon(pts)
    if rot: poly = affinity.rotate(poly, rot, origin=(cx, cy))
    return poly

def bezier(p0, p1, p2, p3=None, n=40):
    out = []
    for i in range(n+1):
        t = i/n
        if p3 is None:
            x = (1-t)**2*p0[0] + 2*(1-t)*t*p1[0] + t*t*p2[0]
            y = (1-t)**2*p0[1] + 2*(1-t)*t*p1[1] + t*t*p2[1]
        else:
            x = (1-t)**3*p0[0] + 3*(1-t)**2*t*p1[0] + 3*(1-t)*t*t*p2[0] + t**3*p3[0]
            y = (1-t)**3*p0[1] + 3*(1-t)**2*t*p1[1] + 3*(1-t)*t*t*p2[1] + t**3*p3[1]
        out.append((x, y))
    return out

def _segments(geom):
    if geom.is_empty: return []
    if isinstance(geom, LineString): return [list(geom.coords)]
    if isinstance(geom, (MultiLineString, GeometryCollection)):
        res = []
        for g in geom.geoms: res += _segments(g)
        return res
    return []

class Plate:
    def __init__(self, w, h, seed=1):
        self.w, self.h = w, h
        self.img = Image.new('L', (w*SS, h*SS), 0)
        self.d = ImageDraw.Draw(self.img)
        self.rnd = random.Random(seed)

    def stroke(self, pts, width=2.0, taper=0.35, wobble=0.0, ink=255):
        if len(pts) < 2: return
        # densificar
        dense = [pts[0]]
        for a, b in zip(pts, pts[1:]):
            L = math.dist(a, b); n = max(1, int(L/3))
            for i in range(1, n+1):
                t = i/n; dense.append((a[0]+(b[0]-a[0])*t, a[1]+(b[1]-a[1])*t))
        n = len(dense)
        for i in range(n-1):
            t = i/(n-1)
            k = 1.0
            if taper > 0:
                edge = min(t, 1-t)
                k = min(1.0, taper + edge*4)
            w = max(0.6, width*k) * SS
            ox = self.rnd.uniform(-wobble, wobble); oy = self.rnd.uniform(-wobble, wobble)
            a = (dense[i][0]*SS+ox, dense[i][1]*SS+oy); b = (dense[i+1][0]*SS+ox, dense[i+1][1]*SS+oy)
            self.d.line([a, b], fill=ink, width=int(round(w)))
            r = w/2
            self.d.ellipse([b[0]-r, b[1]-r, b[0]+r, b[1]+r], fill=ink)

    def outline(self, poly, width=2.4, wobble=0.15):
        polys = [poly] if isinstance(poly, Polygon) else list(getattr(poly, 'geoms', []))
        for pg in polys:
            self.stroke(list(pg.exterior.coords), width, taper=0, wobble=wobble)

    def lines(self, poly, curves, width=1.2, taper=0.4, gap_prob=0.0):
        """Recorta una familia de curvas (polilíneas) a la forma y las dibuja."""
        for c in curves:
            if len(c) < 2: continue
            for seg in _segments(LineString(c).intersection(poly)):
                if gap_prob and self.rnd.random() < gap_prob: continue
                self.stroke(seg, width, taper)

    def hatch(self, poly, angle=45, spacing=6, width=1.1, taper=0.4, wave=0.0, phase=0.0, gap_prob=0.0, jitter=0.6):
        minx, miny, maxx, maxy = poly.bounds
        cx, cy = (minx+maxx)/2, (miny+maxy)/2
        R = math.hypot(maxx-minx, maxy-miny)/2 + 10
        a = math.radians(angle)
        dx, dy = math.cos(a), math.sin(a)
        nx, ny = -dy, dx
        curves = []
        k = -R + phase
        while k <= R:
            off = k + self.rnd.uniform(-jitter, jitter)
            pts = []
            steps = 40
            for i in range(steps+1):
                s = -R + 2*R*i/steps
                wv = wave*math.sin(s/18.0) if wave else 0
                pts.append((cx + dx*s + nx*(off+wv), cy + dy*s + ny*(off+wv)))
            curves.append(pts)
            k += spacing
        self.lines(poly, curves, width, taper, gap_prob)

    def tone(self, poly, level, angle=45, base=7.0, width=1.0):
        """Tonos 1..4: claro → oscuro (cruzado)."""
        if level <= 0: return
        sp = {1: base*1.6, 2: base, 3: base*0.75, 4: base*0.62}[level]
        self.hatch(poly, angle, sp, width*(1.0 if level < 3 else 1.15))
        if level >= 3: self.hatch(poly, angle+70, sp*1.25, width*0.9)
        if level >= 4: self.hatch(poly, angle-40, sp*1.6, width*0.8)

    def dots(self, poly, spacing=7, r=0.9):
        minx, miny, maxx, maxy = poly.bounds
        y = miny
        row = 0
        while y < maxy:
            x = minx + (spacing/2 if row % 2 else 0)
            while x < maxx:
                if poly.contains(Point(x, y)):
                    rr = r*SS
                    self.d.ellipse([x*SS-rr, y*SS-rr, x*SS+rr, y*SS+rr], fill=255)
                x += spacing
            y += spacing*0.87; row += 1

    def erase(self, poly):
        """Pinta 'papel' (borra) dentro de la forma: para superponer objetos."""
        polys = [poly] if isinstance(poly, Polygon) else list(getattr(poly, 'geoms', []))
        for pg in polys:
            self.d.polygon([(x*SS, y*SS) for x, y in pg.exterior.coords], fill=0)

    def save(self, path, size=None, color=(255, 255, 255)):
        img = self.img.resize((size or self.w, int((size or self.w)*self.h/self.w)), Image.LANCZOS)
        rgba = Image.new('RGBA', img.size, color + (0,))
        rgba.putalpha(img)
        rgba.save(path)
        return rgba

def preview(path, out, ink=(43, 79, 160), paper=(250, 246, 237)):
    im = Image.open(path)
    bg = Image.new('RGB', im.size, paper)
    col = Image.new('RGB', im.size, ink)
    bg.paste(col, (0, 0), im.split()[3])
    bg.save(out)
