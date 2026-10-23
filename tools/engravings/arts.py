from engrave import *
from books import feather
from shapely.ops import unary_union

def cyl_lines(pl, poly, cx, rx, ytop, ybot, n=26, start=0.0, bulge=0, width=0.9):
    curves = []
    for i in range(n+1):
        th = math.pi*(1 - (start + (1-start)*i/n))
        x = cx + rx*math.cos(th)
        curves.append([(x, ytop - 20), (x + bulge*math.cos(th)*0.3, (ytop+ybot)/2), (x, ybot + 30)])
    pl.lines(poly, curves, width=width, taper=0.3)

def inkwell():
    pl = Plate(600, 600, seed=7)
    cx = 270
    body = unary_union([P([(140, 350), (400, 350), (430, 500), (110, 500)]), ellipse(cx, 500, 160, 30), ellipse(cx, 350, 130, 26)])
    shoulder = unary_union([ellipse(cx, 352, 130, 26)])
    neck = unary_union([P([(225, 300), (315, 300), (315, 345), (225, 345)]), ellipse(cx, 300, 45, 11), ellipse(cx, 345, 45, 11)])
    # sombra en el piso
    pl.hatch(P([(120, 520), (560, 520), (600, 480), (420, 470), (430, 505)]), angle=0, spacing=3.5, width=0.7)
    pl.erase(body)
    # vidrio oscuro: líneas verticales que siguen el cilindro, más densas a la derecha
    cyl_lines(pl, body, cx, 150, 340, 520, n=34, start=0.25, width=1.0)
    right = P([(cx+40, 300), (460, 300), (460, 540), (cx+40, 540)]).intersection(body)
    pl.hatch(right, angle=70, spacing=5, width=0.8)
    pl.hatch(P([(100, 455), (460, 455), (460, 540), (100, 540)]).intersection(body), angle=-20, spacing=5.5, width=0.8)
    # reflejo
    pl.erase(P([(175, 380), (190, 375), (185, 470), (170, 472)]))
    pl.outline(body, 2.6)
    pl.stroke([(p[0], p[1]) for p in ellipse(cx, 352, 130, 26).exterior.coords][0:46], 1.6, taper=0.3)
    pl.erase(neck)
    cyl_lines(pl, neck, cx, 45, 300, 345, n=12, start=0.3, width=0.9)
    pl.outline(neck, 2.2)
    mouth = ellipse(cx, 300, 40, 9)
    pl.tone(mouth, 4, angle=10, base=4)
    pl.outline(mouth, 1.6)
    feather(pl, (cx+6, 300), (520, 40), curve=(-40, -10), width=34)
    pl.erase(P([(cx-30, 298), (cx+40, 298), (cx+40, 330), (cx-30, 330)]).intersection(neck))
    cyl_lines(pl, P([(cx-30, 296), (cx+40, 296), (cx+40, 330), (cx-30, 330)]).intersection(neck), cx, 45, 300, 345, n=12, start=0.3, width=0.9)
    pl.outline(neck, 2.2); pl.tone(ellipse(cx, 300, 40, 9), 3, angle=10, base=4); pl.outline(ellipse(cx, 300, 40, 9), 1.6)
    pl.stroke([(cx+6, 300), (cx+20, 270)], 2.4)
    return pl

def ring(cx, cy, r1, r2):
    return ellipse(cx, cy, r1, r1).difference(ellipse(cx, cy, r2, r2))

def concentric(pl, poly, cx, cy, r0, r1, step, width=0.8, arc=(0, 360)):
    curves = []
    r = r0
    while r <= r1:
        pts = [(cx + r*math.cos(math.radians(a)), cy + r*math.sin(math.radians(a))) for a in range(arc[0], arc[1]+1, 3)]
        curves.append(pts); r += step
    pl.lines(poly, curves, width=width, taper=0.5)

def watch():
    pl = Plate(600, 600, seed=11)
    cx, cy = 300, 330
    # cadena
    chain = bezier((cx, 130), (120, 120), (70, 300), (90, 520), n=26)
    for i, (x, y) in enumerate(chain[2:]):
        e = ellipse(x, y, 11, 6, rot=(i % 2)*90 + 30)
        pl.erase(e.buffer(1)); pl.outline(e, 1.6)
    # corona y arco
    bow = ellipse(cx, 120, 34, 30).difference(ellipse(cx, 120, 24, 21))
    pl.erase(bow); pl.outline(bow, 2.0); concentric(pl, bow, cx, 120, 24, 34, 3.5, 0.7, arc=(-60, 120))
    crown = P([(cx-22, 140), (cx+22, 140), (cx+22, 172), (cx-22, 172)])
    pl.erase(crown)
    for k in range(-18, 20, 5): pl.stroke([(cx+k, 142), (cx+k, 170)], 1.1, taper=0)
    pl.outline(crown, 2.0)
    stem = P([(cx-10, 172), (cx+10, 172), (cx+10, 168+20), (cx-10, 168+20)])
    pl.erase(stem); pl.hatch(stem, 90, 4, 0.8); pl.outline(stem, 1.8)
    case = ring(cx, cy, 160, 138)
    pl.erase(ellipse(cx, cy, 161, 161))
    concentric(pl, case, cx, cy, 140, 158, 3.2, 0.8)
    pl.hatch(case.intersection(P([(cx-20, cy-40), (cx+200, cy-200), (cx+250, cy+250), (cx-250, cy+250)])), angle=30, spacing=4.2, width=0.8)
    pl.outline(ellipse(cx, cy, 160, 160), 2.8); pl.outline(ellipse(cx, cy, 138, 138), 1.8)
    dial_ring = ring(cx, cy, 138, 128)
    concentric(pl, dial_ring, cx, cy, 129, 137, 2.6, 0.6)
    pl.outline(ellipse(cx, cy, 128, 128), 1.4)
    for m in range(60):
        a = math.radians(m*6 - 90)
        r1 = 120; r2 = 104 if m % 5 == 0 else 114
        pl.stroke([(cx + r1*math.cos(a), cy + r1*math.sin(a)), (cx + r2*math.cos(a), cy + r2*math.sin(a))], 2.6 if m % 5 == 0 else 1.0, taper=0)
    pl.stroke(list(ellipse(cx, cy, 98, 98, n=120).exterior.coords), 0.8, taper=0)
    # subesfera
    sub = ellipse(cx, cy+55, 26, 26)
    pl.outline(sub, 1.2)
    for m in range(12):
        a = math.radians(m*30)
        pl.stroke([(cx + 22*math.cos(a), cy+55 + 22*math.sin(a)), (cx + 17*math.cos(a), cy+55 + 17*math.sin(a))], 1.0, taper=0)
    # agujas
    def hand(angle, length, w):
        a = math.radians(angle - 90)
        tip = (cx + length*math.cos(a), cy + length*math.sin(a))
        nx, ny = -math.sin(a), math.cos(a)
        poly = P([(cx - 14*math.cos(a) + nx*2, cy - 14*math.sin(a) + ny*2), (cx + nx*w, cy + ny*w), tip, (cx - nx*w, cy - ny*w), (cx - 14*math.cos(a) - nx*2, cy - 14*math.sin(a) - ny*2)])
        pl.tone(poly, 4, angle=angle, base=3); pl.outline(poly, 1.4)
    hand(300, 70, 7); hand(52, 100, 5)
    pl.erase(ellipse(cx, cy, 8, 8)); pl.outline(ellipse(cx, cy, 8, 8), 1.6)
    # sombra
    pl.hatch(ellipse(cx+30, cy+175, 150, 16).difference(ellipse(cx, cy, 162, 162)), angle=0, spacing=3.4, width=0.7)
    return pl

def key():
    pl = Plate(700, 360, seed=5)
    cx, cy = 130, 180
    bow = ellipse(cx, cy, 82, 82)
    holes = unary_union([ellipse(cx + 30*math.cos(a), cy + 30*math.sin(a), 20, 20) for a in [math.radians(t) for t in (0, 120, 240)]] + [ellipse(cx, cy, 14, 14)])
    shaft = unary_union([P([(205, 168), (600, 168), (600, 192), (205, 192)]), P([(225, 156), (250, 156), (250, 204), (225, 204)]), P([(270, 160), (282, 160), (282, 200), (270, 200)])])
    bit = P([(540, 192), (600, 192), (600, 270), (585, 270), (585, 245), (570, 245), (570, 280), (552, 280), (552, 250), (540, 250)])
    whole = unary_union([bow.difference(holes), shaft, bit])
    # sombra
    pl.hatch(affinity.translate(whole, 14, 16).difference(whole), angle=-20, spacing=3.6, width=0.75)
    concentric(pl, bow.difference(holes), cx, cy, 50, 82, 3.4, 0.85)
    pl.hatch(bow.difference(holes).intersection(P([(cx, cy), (cx+120, cy), (cx+120, cy+120), (cx-120, cy+120)])), angle=35, spacing=4.5, width=0.8)
    pl.hatch(shaft, angle=0, spacing=3.4, width=1.0)
    pl.hatch(shaft.intersection(P([(200, 181), (610, 181), (610, 210), (200, 210)])), angle=60, spacing=4.5, width=0.75)
    pl.hatch(bit, angle=90, spacing=4.4, width=0.9)
    pl.hatch(bit, angle=30, spacing=6, width=0.7)
    pl.outline(whole, 2.6)
    pl.outline(holes, 1.8)
    return pl

def magnifier():
    pl = Plate(600, 600, seed=9)
    cx, cy = 250, 250
    handle = P([(330, 370), (370, 330), (540, 500), (520, 540), (500, 540)]).buffer(0)
    handle = affinity.rotate(P([(-24, 0), (24, 0), (20, 230), (-20, 230)]), -45, origin=(0, 0))
    handle = affinity.translate(handle, 360, 360)
    ferrule = affinity.translate(affinity.rotate(P([(-30, -10), (30, -10), (30, 40), (-30, 40)]), -45, origin=(0, 0)), 352, 352)
    pl.hatch(affinity.translate(handle, 10, 14).difference(handle), angle=0, spacing=3.4, width=0.7)
    pl.erase(handle)
    pl.hatch(handle, angle=45, spacing=5, width=0.9)
    pl.hatch(handle.intersection(affinity.translate(handle, 14, -14)), angle=-20, spacing=6, width=0.7)
    for k in range(0, 200, 26):
        pass
    pl.outline(handle, 2.4)
    pl.erase(ferrule); pl.hatch(ferrule, angle=-45, spacing=3.6, width=0.9); pl.outline(ferrule, 2.2)
    rim = ring(cx, cy, 150, 128)
    pl.erase(ellipse(cx, cy, 151, 151))
    concentric(pl, rim, cx, cy, 129, 149, 3.2, 0.8)
    pl.hatch(rim.intersection(P([(cx-200, cy), (cx+200, cy-200), (cx+250, cy+250), (cx-250, cy+250)])), angle=-35, spacing=4.4, width=0.8)
    pl.outline(ellipse(cx, cy, 150, 150), 2.8); pl.outline(ellipse(cx, cy, 128, 128), 1.8)
    # brillos en el cristal
    for r, a0, a1, w in ((105, 200, 250, 2.2), (92, 205, 235, 1.4), (110, 25, 55, 1.4)):
        pl.stroke([(cx + r*math.cos(math.radians(a)), cy + r*math.sin(math.radians(a))) for a in range(a0, a1+1, 3)], w, taper=0.2)
    crescent = ellipse(cx, cy, 127, 127).difference(ellipse(cx-22, cy-22, 124, 124))
    pl.hatch(crescent, angle=-45, spacing=4.2, width=0.7)
    return pl

def open_book():
    pl = Plate(800, 520, seed=13)
    sx, top, bot = 400, 120, 430
    def page(side):
        s = 1 if side > 0 else -1
        outer = sx + s*330
        upper = bezier((sx, top+20), (sx + s*90, top-30), (sx + s*250, top-10), (outer, top+10), n=30)
        lower = bezier((outer, bot), (sx + s*250, bot-20), (sx + s*90, bot-10), (sx, bot+20), n=30)
        return P(upper + lower), upper, lower
    cover = P([(40, 160), (760, 160), (770, 455), (400, 470), (30, 455)])
    pl.hatch(affinity.translate(cover, 0, 16).difference(cover), angle=0, spacing=3.4, width=0.7)
    pl.erase(cover)
    pl.tone(cover, 3, angle=10, base=5); pl.outline(cover, 2.4)
    for side in (-1, 1):
        stack, _, _ = page(side)
        stack = affinity.translate(stack, 0, 12)
        pl.erase(stack)
        pl.hatch(stack, angle=-2*side, spacing=2.6, width=0.55)
        pl.outline(stack, 1.6)
    for side in (-1, 1):
        pg, upper, lower = page(side)
        pl.erase(pg)
        # renglones de texto que siguen la curva de la hoja
        for k in range(1, 15):
            t = k/15
            line = [(upper[i][0]*(1-t) + lower[::-1][i][0]*t, upper[i][1]*(1-t) + lower[::-1][i][1]*t) for i in range(len(upper))]
            seg = line[3:-3] if side > 0 else line[3:-3]
            if k % 5 == 0: seg = seg[: len(seg)//2 + 3] if side > 0 else seg[len(seg)//2 - 3:]
            pl.lines(pg, [seg], width=1.0, taper=0.15, gap_prob=0.0)
        gutter = P([(sx - side*0, top-40), (sx + side*55, top-40), (sx + side*55, bot+40), (sx, bot+40)]).intersection(pg)
        pl.erase(gutter)
        pl.hatch(gutter, angle=90, spacing=3.6, width=0.8)
        pl.outline(pg, 2.2)
    pl.stroke([(sx, top+20), (sx, bot+20)], 2.4, taper=0.2)
    return pl

def laurel():
    pl = Plate(600, 400, seed=17)
    for side in (-1, 1):
        stem = bezier((300, 360), (300 + side*190, 330), (300 + side*250, 160), (300 + side*170, 40), n=60)
        pl.stroke(stem, 2.4, taper=0.2)
        for i in range(4, 58, 5):
            px, py = stem[i]; qx, qy = stem[i+1]
            dx, dy = qx-px, qy-py; L = math.hypot(dx, dy) or 1; ux, uy = dx/L, dy/L
            for k in (1, -1):
                nx, ny = -uy*k, ux*k
                length = 46 - i*0.25
                ang = 0.55
                tip = (px + (ux*math.cos(ang) + nx*math.sin(ang))*length, py + (uy*math.cos(ang) + ny*math.sin(ang))*length)
                mid = ((px+tip[0])/2, (py+tip[1])/2)
                mx, my = tip[0]-px, tip[1]-py; ml = math.hypot(mx, my); px2, py2 = -my/ml, mx/ml
                wdt = length*0.22
                leaf = P(bezier((px, py), (mid[0]+px2*wdt*1.6, mid[1]+py2*wdt*1.6), tip, n=14) + bezier(tip, (mid[0]-px2*wdt*1.6, mid[1]-py2*wdt*1.6), (px, py), n=14))
                pl.erase(leaf)
                half = P(bezier((px, py), (mid[0]+px2*wdt*1.6, mid[1]+py2*wdt*1.6), tip, n=14) + [(px, py)])
                pl.hatch(half, angle=math.degrees(math.atan2(my, mx)) + 60, spacing=3.2, width=0.7)
                pl.outline(leaf, 1.5)
                pl.stroke([(px, py), tip], 1.0, taper=0.3)
    # moño
    pl.outline(ellipse(300, 360, 12, 9), 1.8)
    return pl

if __name__ == '__main__':
    for name, fn, size in (('inkwell', inkwell, 600), ('watch', watch, 600), ('key', key, 700), ('magnifier', magnifier, 600), ('open_book', open_book, 800), ('laurel', laurel, 600)):
        pl = fn(); pl.save(f'{name}.png', size); preview(f'{name}.png', f'{name}_prev.png')
        print(name)
