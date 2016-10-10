(function( ColorZebra, $, undefined ) {
    ColorZebra.colorMaps = {
        'Grey' : new ColorZebra.ColorMap(
            'Grey',
            'Grey scale',
            [ [  0, 0, 0],
              [100, 0, 0] ],
            2
        ),
        
        'White-hot' : new ColorZebra.ColorMap(
            'White-hot',
            'Black-Red-Yellow-White heat colour map',
            [ [  5,  0,  0],
              [ 15, 37, 21],
              [ 25, 49, 37],
              [ 35, 60, 50],
              [ 45, 72, 60],
              [ 55, 80, 70],
              [ 65, 56, 73],
              [ 75, 31, 78],
              [ 85,  9, 84],
              [100,  0,  0] ],
            3
        ),
        
        'Glow' : new ColorZebra.ColorMap(
            'Glow',
            'Black-Red-Yellow heat colour map',
            [ [ 5,   0,  0],
              [15,  37, 21],
              [25,  49, 37],
              [35,  60, 50],
              [45,  72, 60],
              [55,  80, 70],
              [65,  56, 73],
              [75,  31, 78],
              [85,   9, 84],
              [98, -16, 93] ],
            3
        ),
        
        'Fern' : new ColorZebra.ColorMap(
            'Fern',
            'Colour Map along the green edge of CIELAB space',
            [ [ 5,  -9,  5],
              [15, -23, 20],
              [25, -31, 31],
              [35, -39, 39],
              [45, -47, 47],
              [55, -55, 55],
              [65, -63, 63],
              [75, -71, 71],
              [85, -79, 79],
              [95, -38, 90] ],
            3
        ),
        
        'Sky' : new ColorZebra.ColorMap(
            'Sky',
            'Blue shades running vertically up the blue edge of CIELAB space',
            [ [ 5,  30,  -52],
              [15,  49,  -80],
              [25,  64, -105],
              [35,  52, -103],
              [45,  26,  -87],
              [55,   6,  -72],
              [65, -12,  -56],
              [75, -29,  -40],
              [85, -44,  -24],
              [95, -31,   -9] ],
            3
        ),
        
        'Twilight' : new ColorZebra.ColorMap(
            'Twilight',
            'Blue-Pink-Light Pink colour map',
            [ [ 5, 30,  -52],
              [15, 49,  -80],
              [25, 64, -105],
              [35, 73, -105],
              [45, 81,  -88],
              [55, 90,  -71],
              [65, 85,  -55],
              [75, 58,  -38],
              [85, 34,  -23],
              [95, 10,   -7] ],
            3
        ),
        
        'Sunrise' : new ColorZebra.ColorMap(
            'Sunrise',
            'Blue-Magenta-Orange-Yellow highly saturated colour map',
            [ ColorZebra.Color.LCHtoLAB(10, 78, -60),  
              ColorZebra.Color.LCHtoLAB(20, 100, -60),
              ColorZebra.Color.LCHtoLAB(30, 78, -40),
              ColorZebra.Color.LCHtoLAB(40, 74, -20),                
              ColorZebra.Color.LCHtoLAB(50, 80, 0),                 
              ColorZebra.Color.LCHtoLAB(60, 80, 20),
              ColorZebra.Color.LCHtoLAB(70, 72, 50),
              ColorZebra.Color.LCHtoLAB(80, 84, 77),
              ColorZebra.Color.LCHtoLAB(95, 90, 95) ],
            3
         ),
        
        'Lake' : new ColorZebra.ColorMap(
            'Lake',
            'Blue-Green-Yellow-White colour map',
            [ [ 15,  50,  -65],
              [ 35,  67, -100],
              [ 45, -14,  -30],
              [ 60, -55,   60],
              [ 85, -10,   80],
              [ 95, -17,   50],
              [100,   0,    0] ],
            3
         ),
        
        'Morning Mist' : new ColorZebra.ColorMap(
            'Morning Mist',
            'A geographical colour map, best used with relief shading',
            [ ColorZebra.Color.LCHtoLAB(60, 20, 180),
              ColorZebra.Color.LCHtoLAB(65, 30, 135),
              ColorZebra.Color.LCHtoLAB(70, 35, 75),
              ColorZebra.Color.LCHtoLAB(75, 45, 85),
              ColorZebra.Color.LCHtoLAB(80, 22, 90), 
              [85, 0, 0] ],
            3
         ),
        
        'Dawn' : new ColorZebra.ColorMap(
            'Dawn',
            'A more saturated geographical colour map, best used with relief shading',
            [ ColorZebra.Color.LCHtoLAB(65, 50, 135),
              ColorZebra.Color.LCHtoLAB(75, 45, 75),
              ColorZebra.Color.LCHtoLAB(80, 45, 85),
              ColorZebra.Color.LCHtoLAB(85, 22, 90),        
              [90, 0, 0] ],
            3
         ),
         
        'Water' : new ColorZebra.ColorMap(
            'Water',
            'A water depth colour map',
            [ ColorZebra.Color.LCHtoLAB(50, 35, -95),
              ColorZebra.Color.LCHtoLAB(60, 25, -95),
              ColorZebra.Color.LCHtoLAB(70, 25, -95),
              ColorZebra.Color.LCHtoLAB(80, 20, -95),
              [95, 0, 0] ],
            3
         )
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));