# Grabados de Libreta

Las ilustraciones en tinta azul de la app se generan con estos scripts: formas simples
rellenas con sombreado de líneas paralelas (y cruzadas en las sombras), como un grabado.

```bash
pip install shapely numpy pillow
python3 books.py   # pila de libros con pluma
python3 arts.py    # tintero, reloj, llave, lupa, libro abierto, laurel
```

Se exportan como máscaras alfa y la app las tiñe con `ColorFilter.tint` (azul en claro,
celeste suave en oscuro). Son obra original de este proyecto.
