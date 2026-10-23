from engrave import *
from shapely.ops import unary_union

def book(pl, x, y, L, H, D, rot=0, spine_bands=True, seed=0, title_band=True):
    vx, vy = 0.62*D, -0.36*D
    p0 = (x, y)
    # caras
    top = Polygon([(x, y), (x+L, y), (x+L+vx, y+vy), (x+vx, y+vy)])
    bulge = 7
    front_pts = [(x, y)] + [(x+L*t, y - 0*bulge) for t in [0.5]] + [(x+L, y)] + \
        [(x+L+2, y+H*0.5)] + [(x+L, y+H)] + [(x+L*t, y+H+bulge*math.sin(math.pi*t)) for t in [i/12 for i in range(12, -1, -1)]] + [(x-2, y+H*0.5)]
    front = Polygon(front_pts)
    side = Polygon([(x+L, y), (x+L+vx, y+vy), (x+L+vx, y+vy+H), (x+L, y+H)])
    group = [top, front, side]
    if rot:
        c = (x+L/2, y+H/2)
        group = [affinity.rotate(g, rot, origin=c) for g in group]
    top, front, side = group
    whole = unary_union(group).buffer(1.5)
    pl.erase(whole)
    # tapa superior: luz, pocas líneas paralelas al lomo
    ang = rot
    pl.hatch(top, angle=ang + (-30), spacing=12, width=0.8, gap_prob=0.35)
    # lomo: tono medio, líneas horizontales
    pl.hatch(front, angle=ang, spacing=5.2, width=1.05)
    # volumen del lomo: sombra cruzada en la mitad inferior y borde derecho
    def T0(px, py):
        if not rot: return (px, py)
        pt = affinity.rotate(Point(px, py), rot, origin=(x+L/2, y+H/2)); return (pt.x, pt.y)
    low = Polygon([T0(x-5, y+H*0.62), T0(x+L+5, y+H*0.62), T0(x+L+5, y+H+12), T0(x-5, y+H+12)]).intersection(front)
    pl.hatch(low, angle=ang+62, spacing=6.5, width=0.8)
    rightband = Polygon([T0(x+L*0.9, y-2), T0(x+L+5, y-2), T0(x+L+5, y+H+12), T0(x+L*0.9, y+H+12)]).intersection(front)
    pl.hatch(rightband, angle=ang-58, spacing=6, width=0.8)
    # cantos de páginas: líneas finas y densas siguiendo la profundidad
    inner = side.buffer(-5)
    pl.hatch(inner if not inner.is_empty else side, angle=ang - 30.1, spacing=2.6, width=0.55, taper=0.6)
    # bandas del lomo
    def T(px, py):
        if not rot: return (px, py)
        pt = affinity.rotate(Point(px, py), rot, origin=(x+L/2, y+H/2)); return (pt.x, pt.y)
    if spine_bands:
        for bx in [0.12, 0.17, 0.83, 0.88]:
            pl.stroke([T(x+L*bx, y+3), T(x+L*bx, y+H+5)], 2.0, taper=0.2)
    if title_band:
        lab = Polygon([T(x+L*0.36, y+H*0.25), T(x+L*0.64, y+H*0.25), T(x+L*0.64, y+H*0.75), T(x+L*0.36, y+H*0.75)])
        pl.erase(lab)
        pl.outline(lab, 1.6)
        pl.stroke([T(x+L*0.41, y+H*0.5), T(x+L*0.59, y+H*0.5)], 1.4)
    pl.outline(top, 2.2); pl.outline(front, 2.6); pl.outline(side, 2.2)
    return whole

def feather(pl, base, tip, curve=(0, -60), width=46, seed=3):
    bx, by = base; tx, ty = tip
    cx, cy = (bx+tx)/2 + curve[0], (by+ty)/2 + curve[1]
    rachis = bezier(base, (cx, cy), tip, n=60)
    # vanos
    left, right = [], []
    n = len(rachis)
    for i, (px, py) in enumerate(rachis):
        t = i/(n-1)
        if i < n-1: dx, dy = rachis[i+1][0]-px, rachis[i+1][1]-py
        else: dx, dy = px-rachis[i-1][0], py-rachis[i-1][1]
        L = math.hypot(dx, dy) or 1
        nx, ny = -dy/L, dx/L
        w = width*math.sin(math.pi*min(1, max(0, (t-0.18)/0.82)))**0.7 if t > 0.18 else 0
        left.append((px+nx*w, py+ny*w)); right.append((px-nx*w*0.75, py-ny*w*0.75))
    vane = Polygon(left + right[::-1]).buffer(0)
    pl.erase(vane.buffer(2))
    # barbas: trazos oblicuos desde el raquis
    for i in range(int(n*0.2), n-1, 1):
        px, py = rachis[i]
        dx, dy = rachis[i+1][0]-px, rachis[i+1][1]-py
        L = math.hypot(dx, dy) or 1
        ux, uy = dx/L, dy/L
        for side, k in ((1, 1.0), (-1, 0.75)):
            nx, ny = -uy*side, ux*side
            ex, ey = px + (nx*0.85 + ux*0.55)*width*1.3*k, py + (ny*0.85 + uy*0.55)*width*1.3*k
            pl.lines(vane, [[(px, py), (ex, ey)]], width=0.75, taper=0.5)
    # muescas
    # sombra en el vano inferior
    shade = Polygon(right + rachis[::-1]).buffer(0)
    pl.hatch(shade, angle=20, spacing=4.5, width=0.7)
    pl.outline(vane, 1.6, wobble=0.3)
    pl.stroke(rachis, 2.6, taper=0.15)

if __name__ == '__main__':
    pl = Plate(1000, 760, seed=4)
    book(pl, 150, 560, 600, 95, 210)
    # sombra proyectada del libro de arriba sobre la tapa de abajo
    pl.hatch(Polygon([(200, 556), (745, 556), (860, 486), (830, 470), (230, 548)]), angle=-30, spacing=4, width=0.8)
    pl.hatch(Polygon([(200, 556), (745, 556), (860, 486), (830, 470), (230, 548)]), angle=30, spacing=5, width=0.7)
    book(pl, 215, 455, 520, 100, 190, rot=-3.5)
    pl.hatch(Polygon([(255, 452), (690, 452), (800, 392), (780, 380), (280, 446)]), angle=-30, spacing=4, width=0.8)
    pl.hatch(Polygon([(255, 452), (690, 452), (800, 392), (780, 380), (280, 446)]), angle=30, spacing=5, width=0.7)
    book(pl, 250, 362, 430, 80, 170, rot=2.5, spine_bands=False)
    feather(pl, (330, 330), (860, 140), curve=(30, -70), width=40)
    # sombra en el suelo
    pl.hatch(Polygon([(150, 657), (752, 657), (880, 580), (930, 580), (800, 672), (120, 672)]), angle=0, spacing=3.4, width=0.75)
    pl.save('books.png', 1000)
    preview('books.png', 'books_prev.png')
