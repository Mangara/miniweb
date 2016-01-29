(function( ColorZebra, $, undefined ) {
    ColorZebra.Color = function() {}
    
    ColorZebra.Color.desaturateLAB = function(cielab) {
        return [cielab[0], cielab[1] / 3, cielab[2] / 3];
    }
    
    ColorZebra.Color.LABtoXYZ = function(cielab) {
        // Convert to CIEXYZ. Formulas and numbers from Wikipedia:
        // https://en.wikipedia.org/wiki/Lab_color_space
        // Using conversion constants corresponding to illuminant D65
        var fY = 0.00862068965 * (cielab[0] + 16);
        var X = 0.95047 * f_inverse(fY + 0.002 * cielab[1]);
        var Y =           f_inverse(fY);
        var Z = 1.08883 * f_inverse(fY - 0.005 * cielab[2]);

        return [X, Y, Z];
        
        function f_inverse(t) {
            if (t > 0.20689655172) {
                return Math.pow(t, 3);
            } else {
                return 0.12841854934 * (t - 0.13793103448);
            }
        }
    }
    
    ColorZebra.Color.LABtoRGB = function(cielab) {
        // Convert to CIEXYZ
        var xyz = ColorZebra.Color.LABtoXYZ(cielab);
        
        // Convert to RGB. Formulas and numbers from Wikipedia:
        // https://en.wikipedia.org/wiki/SRGB#The_forward_transformation_.28CIE_xyY_or_CIE_XYZ_to_sRGB.29
        var R = correctGamma( 3.2406 * xyz[0] - 1.5372 * xyz[1] - 0.4986 * xyz[2]);
        var G = correctGamma(-0.9689 * xyz[0] + 1.8758 * xyz[1] + 0.0415 * xyz[2]);
        var B = correctGamma( 0.0557 * xyz[0] - 0.2040 * xyz[1] + 1.0570 * xyz[2]);
        
        // Clamp out-of-gamut colors
        R = Math.max(0, Math.min(1, R));
        G = Math.max(0, Math.min(1, G));
        B = Math.max(0, Math.min(1, B));
        
        return [R, G, B];
        
        function correctGamma(t) {
            if (t <= 0.0031308) {
                return 12.92 * t;
            } else {
                return 1.055 * Math.pow(t, 0.41666666666) - 0.055;
            }
        }
    }
    
    ColorZebra.Color.LABtoIntegerRGB = function(cielab) {
        var rgb = ColorZebra.Color.LABtoRGB(cielab);
        return [Math.round(255 * rgb[0]), Math.round(255 * rgb[1]), Math.round(255 * rgb[2])];
    }
    
    ColorZebra.Color.LABtoCSS = function(cielab) {
        var rgb = ColorZebra.Color.LABtoIntegerRGB(cielab);
        return "rgb(" + rgb[0] + "," + rgb[1] + "," + rgb[2] + ")";
    }
    
    ColorZebra.Color.LCHtoLAB = function(l, c, h) {
        var theta = Math.PI * h / 180;
        return [l, c * Math.cos(theta), c * Math.sin(theta)]
    }

    ColorZebra.Color.testLABtoRGB = function() {
        // First 10 RGB values randomly generated from random.org, then added all (0, 255)-combinations
        // CIELAB and XYZ color values from colorhexa.com
        var cielab = [ [59.653, 37.295, -58.801], [43.843, 50.226, -75.636], [37.15, 37.831, -75.353], [72.702, -68.674, 51.87], [65.978, 7.656, -52.21], [11.76, 26.804, -20.84], [34.274, 67.411, -86.313], [53.807, 79.294, -27.645], [82.057, -66.64, 65.042], [53.738, 70.077, 48.852], [0, 0, 0], [100, -0, -0.009], [53.239, 80.09, 67.201], [87.735, -86.183, 83.18], [32.299, 79.191, -107.865], [97.139, -21.558, 94.477], [60.324, 98.235, -60.835], [91.114, -48.083, -14.139] ];
        var xyz    = [ [0.36487, 0.2774, 0.92234], [0.22254, 0.1373, 0.77817], [0.14461, 0.09619, 0.63378], [0.234647, 0.44712, 0.1405], [0.35776, 0.35296, 0.98688], [0.02389, 0.0137, 0.04413], [0.17437, 0.0814, 0.7046], [0.41784, 0.21793, 0.44123], [0.34312, 0.60403, 0.15319], [0.38726, 0.21729, 0.04951], [0, 0, 0], [0.95047, 1, 1.08897], [0.41242, 0.21266, 0.01933], [0.35758, 0.71516, 0.11919], [0.18046, 0.07219, 0.95044], [0.77, 0.92781, 0.13853], [0.59289, 0.28484, 0.96978], [0.53804, 0.78734, 1.06964] ];
        var rgb    = [ [148, 125, 248], [98, 77, 232], [15, 73, 212], [10, 206, 75], [99, 161, 254], [52, 14, 60], [84, 30, 223], [231, 41, 178], [92, 232, 68], [243, 52, 48], [0, 0, 0], [255, 255, 255], [255, 0, 0], [0, 255, 0], [0, 0, 255], [255, 255, 0], [255, 0, 255], [0, 255, 255] ];

        // Test XYZ
        var passed = 0;

        console.log('Testing XYZ color conversions.');

        for (var i = 0, max = cielab.length; i < max; i++) {
            var expected = xyz[i];
            var c = new ColorZebra.Color(cielab[i]);
            var result = c.toXYZ();

            if (round(expected[0]) === round(result[0]) && round(expected[1]) === round(result[1]) && round(expected[2]) === round(result[2])) {
                passed++;
            } else {
                console.log('Test ' + i + ' failed: Color ' + cielab[i] + ' converted to ' + result + ', where ' + expected + ' was expected.');
            }
        }

        console.log(passed + ' tests passed.');
        console.log('');
        console.log('Testing RGB color conversions.');

        // Test RGB
        var passed = 0;

        for (var i = 0, max = cielab.length; i < max; i++) {
            var expected = rgb[i];
            var result = ColorZebra.Color.LABtoIntegerRGB(cielab[i]);

            if (expected[0] === result[0] && expected[1] === result[1] && expected[2] === result[2]) {
                passed++;
            } else {
                console.log('Test ' + i + ' failed: Color ' + cielab[i] + ' converted to ' + result + ', where ' + expected + ' was expected.');
            }
        }

        console.log(passed + ' tests passed.');

        function round(t) {
            return Math.round(10000 * t)/10000;
        }
    }
    
    ColorZebra.Color.testLCHtoLAB = function() {
        var lch = [ [30, 89, -59], [90, 89, 96] ];
        var lab = [ [30, 45.8384, -76.2879], [90, -9.3030, 88.5124] ];
        
        var passed = 0;

        console.log('Testing LCH to LAB color conversions.');

        for (var i = 0, max = lch.length; i < max; i++) {
            var expected = lab[i];
            var result = ColorZebra.Color.LCHtoLAB(lch[i][0], lch[i][1], lch[i][2]);

            if (round(expected[0]) === round(result[0]) && round(expected[1]) === round(result[1]) && round(expected[2]) === round(result[2])) {
                passed++;
            } else {
                console.log('Test ' + i + ' failed: Color ' + lch[i] + ' converted to ' + result + ', where ' + expected + ' was expected.');
            }
        }

        console.log(passed + ' tests passed.');
        
        function round(t) {
            return Math.round(10000 * t)/10000;
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));(function( ColorZebra, $, undefined ) {
    ColorZebra.LinearSpline = function(controlPoints) {
        var points = controlPoints; // Monotonically increasing lightness
        
        this.getColorForLightness = function(l) {
            if (l < points[0][0] || l > points[points.length - 1][0]) {
                return null;
            }
            if (l == points[points.length - 1][0]) {
                return points[points.length - 1];
            }
            
            // Binary search for interval containing l
            // Invariants:
            //    lightness of points[start] <= l
            //    lightness of points[end] > l
            var start = 0, end = points.length - 1;
            
            while (end - start > 1) {
                var middle = Math.floor(start + (end - start) / 2);
                var midVal = points[middle][0];
                
                if (midVal <= l) {
                    start = middle;
                } else {
                    end = middle;
                }
            }
            // Now: lightness of points[start] <= l < lightness of points[start + 1]
            // Linear interpolation
            var fraction = (l - points[start][0]) / (points[end][0] - points[start][0]);
            return [
              l, 
              points[start][1] + fraction * (points[end][1] - points[start][1]),
              points[start][2] + fraction * (points[end][2] - points[start][2])
              ]
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));(function( ColorZebra, $, undefined ) {
    ColorZebra.QuadraticSpline = function(controlPoints) {
        var n = controlPoints.length;
        var points = controlPoints.slice();
        points.unshift(null); // Monotonically increasing lightness, starting with index 1
        var coefficients = computeCoefficients();
        
        this.getColorForLightness = function(l) {
            if (l < points[1][0] || l > points[points.length - 1][0]) {
                return null;
            }
            if (l == points[points.length - 1][0]) {
                return points[points.length - 1];
            }
            
            // Binary search for interval containing l
            // Invariants:
            //    lightness of points[start] <= l
            //    lightness of points[end] > l
            var start = 1, end = points.length - 1;
            
            while (end - start > 1) {
                var middle = Math.floor(start + (end - start) / 2);
                var midVal = points[middle][0];
                
                if (midVal <= l) {
                    start = middle;
                } else {
                    end = middle;
                }
            }
            // Now: lightness of points[start] <= l < lightness of points[start + 1]
            
            // Interval [j - 1, j] depends on control points p_j, p_{j+1}, and p_{j+2}
            // So if the lightness value lies between that of p_{start} and p_{start+1}
            // Then it lies in interval [start - 1, start], which depends on p_{start}, p_{start+1}, and p_{start+2},
            // or in interval [start - 2, start - 1], which depends on p_{start-1}, p_{start}, and p_{start+1}
            
            var sol = null;
            var intervalEnd;
            
            // Try the earlier interval first
            if (start > 1) {
                sol = findLightnessInInterval(l, start - 1);
                intervalEnd = start - 1;
            }

            if (sol === null) {
                sol = findLightnessInInterval(l, start);
                intervalEnd = start;
            }
            
            // We found the correct parameter value: now simply evaluate the spline
            var deboor = deBoor(sol, intervalEnd);
            return [l, deboor[0], deboor[1]];
        }
        
        function findLightnessInInterval(l, intervalEnd) {
            var a = coefficients[intervalEnd][0],
                b = coefficients[intervalEnd][1],
                c = coefficients[intervalEnd][2] - l,
                sol;
                
            if (a === 0) {
                if (b === 0) {
                    return null;
                } else {
                    return nullIfNotInInterval(-c / b);
                }
            } else {
                var D = b * b - 4 * a * c;
                
                if (D < 0) {
                    return null;
                } else if (D == 0) {
                    return nullIfNotInInterval(-b / (2 * a));
                } else {
                    var sqrtD = Math.sqrt(D);
                    var sol = nullIfNotInInterval((-b - sqrtD) / (2 * a));
                    
                    if (sol === null) {
                        return nullIfNotInInterval((-b + sqrtD) / (2 * a));
                    } else {
                        return sol;
                    }
                }
            }
            
            // Only return solutions in the knot-interval
            function nullIfNotInInterval(sol) {
                if ((intervalEnd - 1) <= sol && sol <= intervalEnd) {
                    return sol;
                } else {
                    return null;
                }
            }
        }
        
        function computeCoefficients() {
            // This is a cardinal spline, so the knot vector is implicitly defined as [0, 0, 0, 1, 2, ..., n - 3, n - 2, n - 2, n - 2]
            // coefficients[i] contains the coefficients of the quadratic equation for the piece of the spline with parameter value in [i - 1, i]
            // See misc/QuadraticSpline.ipe for the derivation
            var c = [];
            
            c[1] = [
                ( 2 * points[1][0] - 3 * points[2][0] + points[3][0])/2,
                (-4 * points[1][0] + 4 * points[2][0])/2,
                ( 2 * points[1][0])/2
            ];
            
            var j;
            for (j = 2; j < n - 2; j++) {
                c[j] = [
                    (points[j][0] - 2 * points[j+1][0] + points[j+2][0])/2,
                    (-2 * j * points[j][0] + (4 * j - 2) * points[j+1][0] + (2 - 2 * j) * points[j+2][0])/2,
                    (j * j * points[j][0] + (-2 * j * j + 2 * j + 1) * points[j+1][0] + (j * j - 2 * j + 1) * points[j+2][0])/2
                ];
            }
            
            c[n - 2] = [
                (points[n-2][0] - 3 * points[n-1][0] + 2 * points[n][0])/2,
                ((4 - 2 * n) * points[n-2][0] + (6 * n - 16) * points[n-1][0] + (12 - 4 * n) * points[n][0])/2,
                ((n * n - 4 * n + 4) * points[n-2][0] + (-3 * n * n + 16 * n - 20) * points[n-1][0] + (2 * n * n - 12 * n + 18) * points[n][0])/2
            ];
            
            return c;
        }
        
        var knots = [];
        var k;
        
        for (k = 0; k < n + 3; k++) {
            if (k < 2) {
                knots[k] = 0;
            } else if (k > n) {
                knots[k] = n - 2;
            } else {
                knots[k] = k - 2;
            }
        }
        
        function deBoor(t, k) {
            var a21 = (knots[k + 3] === knots[k + 1] ? 0 : (t - knots[k + 1]) / (knots[k + 3] - knots[k + 1]));
            var a11 = (knots[k + 2] === knots[k] ? 0 : (t - knots[k]) / (knots[k + 2] - knots[k]));
            
            var p21a = (1 - a21) * points[k+1][1] + a21 * points[k+2][1];
            var p21b = (1 - a21) * points[k+1][2] + a21 * points[k+2][2];
            var p11a = (1 - a11) * points[k][1] + a11 * points[k+1][1];
            var p11b = (1 - a11) * points[k][2] + a11 * points[k+1][2];
            
            var a22 = (knots[k + 2] === knots[k + 1] ? 0 : (t - knots[k + 1]) / (knots[k + 2] - knots[k + 1]));
            
            return [
                (1 - a22) * p11a + a22 * p21a,
                (1 - a22) * p11b + a22 * p21b
            ];
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));(function( ColorZebra, $, undefined ) {
    ColorZebra.ColorMap = function(name, description, controlPoints, splineOrder) {
        this.name = name;
        this.description = description;
        
        var points = controlPoints;
        var spline = (splineOrder === 2 ? new ColorZebra.LinearSpline(controlPoints) : new ColorZebra.QuadraticSpline(controlPoints));
        
        this.getLABColor = function(value) {
            return spline.getColorForLightness(normalizeLightness(value));
        }
        
        this.getCSSColor = function(value) {
            return ColorZebra.Color.LABtoCSS(this.getLABColor(value));
        }
        
        function normalizeLightness(value) {
            return points[0][0] + value * (points[points.length - 1][0] - points[0][0]);
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));(function( ColorZebra, $, undefined ) {
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
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));(function( ColorZebra, $, undefined ) {
    function getColors() {
        var colors = [], i;
        
        for (i = 0; i < ColorZebra.numColors; i++) {
            colors.push(ColorZebra.colorMap.getLABColor(i / (ColorZebra.numColors - 1)));
        }
        
        return colors;
    }
    
    ColorZebra.exportIntegerCSV = function() {
        var result = "r,g,b\r\n", i; // Always uses \r\n as per RFC 4180 ( https://tools.ietf.org/html/rfc4180 )
        var colors = getColors(),
            color;
        
        for (i = 0; i < ColorZebra.numColors; i++) {
            color = ColorZebra.Color.LABtoIntegerRGB(colors[i]);
            result += color[0] + ',' + color[1] + ',' + color[2] + '\r\n';
        }
        
        return result;
    }
    
    ColorZebra.exportFloatCSV = function() {
        var result = "r,g,b\r\n", i; // Always uses \r\n as per RFC 4180 ( https://tools.ietf.org/html/rfc4180 )
        var colors = getColors(),
            color;
        
        for (i = 0; i < ColorZebra.numColors; i++) {
            color = ColorZebra.Color.LABtoRGB(colors[i]);
            result += color[0] + ',' + color[1] + ',' + color[2] + '\r\n';
        }
        
        return result;
    }
    
    ColorZebra.exportIPE = function() {
        var result = '<ipestyle name="' + ColorZebra.colorMap.name + '_ColorMap_' + ColorZebra.numColors + '">\n'; // IPE understands \n, even on Windows
        var i;
        var colors = getColors();
        
        console.log('Colors: ' + colors);
        
        for (i = 0; i < ColorZebra.numColors; i++) {
            color = ColorZebra.Color.LABtoRGB(colors[i]);
            result += '<color name="' + ColorZebra.colorMap.name + '_' + i + '" value="' + color[0] + ' ' + color[1] + ' ' + color[2] + '"/>\n';
        }
        
        result += '</ipestyle>';
        
        return result;
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));(function( ColorZebra, $, undefined ) {
    ColorZebra.CMapDrawer = function(theCanvas, theColorMap) {
        var canvas = theCanvas;
        var colorMap = theColorMap;
        
        var desaturate = true;
        
        this.setDesaturate = function(newDesaturate) {
            desaturate = newDesaturate;
        }
        
        this.draw = function() {
            if (canvas === undefined) {
                alert('Canvas undefined');
                return;
            }
            
            var context = canvas.getContext("2d"),
                x,
                width = canvas.width,
                height = canvas.height;
                        
            for (x = 0; x < width; x++) {
                var val = x / (width - 1);
                context.fillStyle = getColor(val);
                context.fillRect(x, 0, 1, height);
            }
        }
        
        function getColor(val) {
            var color = colorMap.getLABColor(val);
            
            if (desaturate) {
                color = ColorZebra.Color.desaturateLAB(color);
            }
            
            return ColorZebra.Color.LABtoCSS(color);
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));(function( ColorZebra, $, undefined ) {
    ColorZebra.Preview = function(theCanvas) {
        var canvas = theCanvas;
        var PI_BY_FOUR = Math.PI / 4;
        
        this.maximize = function() {
            var parent = $(canvas).parent();
            canvas.width = parent.width();
            canvas.height = parent.height() - 4; // No clue why the -4 is necessary.
        }
        
        this.draw = function() {
            if (canvas === undefined) {
                alert('Canvas undefined');
                return;
            }
            
            drawPiecewiseLinear();
        }
        
        function drawPiecewiseLinear() {
            // The test image consists of a sine wave plus a ramp function
            // The sine wave has a wavelength of 8 pixels (which is why we multiply by 2pi/8 = pi/4)
            // The base sine wave has amplitude 0.05, so that it spans 10% of the value range
            // In each column, the amplitude of the sine wave ranges from 0 at the bottom to 0.05 at the top, increasing quadratically
            // In each row, the ramp goes from <z> on the left to (1 - <z>) on the right, where <z> = 0.05 * ((height - y) / height)^2 is the maximum amplitude of the sine wave in that row
            // Drawing it per-pixel is slow, because context fillstyle changes are very expensive (much more than any calculations we're doing).
            // This method draws the same image, except that the amplitude of the sine wave approximates the quadratic modulation with a piecewise linear one.
            // This is ~25 times faster than drawing each pixel for STEPS = 10, and nearly impossible to distinguish visually.
            var STEPS = 10;
            
            var context = canvas.getContext("2d"),
                x,
                width = canvas.width,
                height = canvas.height;
                        
            for (x = 0; x < width; x++) {
                var xt = x / (width - 1); // x mapped to [0, 1]
                var sinVal = Math.sin(x * PI_BY_FOUR);
                
                var my_gradient = context.createLinearGradient(0, 0, 0, height);
                
                for (y = STEPS; y > 0; y--) {
                    var yt = y / STEPS;
                    var amp = 0.05 * yt * yt;
                    var val = amp * sinVal + getRamp(xt, amp);
                    my_gradient.addColorStop(1 - yt, ColorZebra.colorMap.getCSSColor(val));
                }
                
                context.fillStyle = my_gradient;
                context.fillRect(x, 0, 1, height);
            }
        }
        
        function drawQuadratic() {
            // Super-slow way of drawing the test image, for performance comparison
            
            var context = canvas.getContext("2d"),
                x, y,
                width = canvas.width,
                height = canvas.height;
                        
            for (x = 0; x < width; x++) {
                var xt = x / (width - 1); // x mapped to [0, 1]
                var sinVal = Math.sin(x * PI_BY_FOUR);
                
                for (y = 0; y < height; y++) {
                    var yt = (height - y) / (height - 1);
                    var amp = 0.05 * yt * yt;
                    var val = amp * sinVal + getRamp(xt, amp);
                    context.fillStyle = ColorZebra.colorMap.getCSSColor(val);
                    context.fillRect(x, y, 1, 1);
                }
            }
        }
        
        function getRamp(xt, amp) {
            return amp + (1 - 2 * amp) * xt;
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));(function( ColorZebra, $, undefined ) {
    ColorZebra.FixedNumPreview = function(theCanvas) {
        var canvas = theCanvas;
        
        this.maximize = function() {
            var parent = $(canvas).parent();
            canvas.width = parent.width();
            canvas.height = parent.height() - 4; // No clue why the -4 is necessary.
        }
        
        this.draw = function() {
            if (canvas === undefined) {
                alert('Canvas undefined');
                return;
            }
            
            var context = canvas.getContext("2d"),
                i,
                colWidth = canvas.width / ColorZebra.numColors,
                start, end;
                
            // Clear the canvas
            context.clearRect(0, 0, canvas.width, canvas.height);
            
            if (colWidth > 1) {
                // Draw individual colors
                for (i = 0; i < ColorZebra.numColors; i++) {
                    if (colWidth >= 10) {
                        // Discrete full-pixel boundaries
                        start = Math.floor(i * colWidth);
                        end = Math.floor((i + 1) * colWidth) - 1;
                    } else {
                        // Aliased relative boundaries
                        start = i * colWidth;
                        end = (i + 0.9) * colWidth;
                    }
                    
                    var val = i / (ColorZebra.numColors - 1);
                    
                    context.fillStyle = ColorZebra.colorMap.getCSSColor(val);
                    context.fillRect(start, 0, end - start, canvas.height);
                }
            } else {
                // Just draw the colormap
                for (i = 0; i < canvas.width; i++) {
                    var val = i / (canvas.width - 1);
                    
                    context.fillStyle = ColorZebra.colorMap.getCSSColor(val);
                    context.fillRect(i, 0, 1, canvas.height);
                }
            }
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));(function( ColorZebra, $, undefined ) {
    ColorZebra.colorMap = ColorZebra.colorMaps['Lake'];
    ColorZebra.numColors = 12;
    
    ColorZebra.main = function() {
        // Handle on-load stuff
        $(document).ready(function() {
            // Prepare our preview panels
            ColorZebra.mainPreview = new ColorZebra.Preview($('#preview')[0]);
            ColorZebra.mainPreview.maximize();
            ColorZebra.mainPreview.draw();
            
            ColorZebra.fixedNumPreview = new ColorZebra.FixedNumPreview($('#fixednum-preview')[0]);
            ColorZebra.fixedNumPreview.maximize();
            ColorZebra.fixedNumPreview.draw();
            
            // Create all thumbnails
            $('#colormaps>canvas').each(function() {
                var map = ColorZebra.colorMaps[this.id];
                map.canvas = new ColorZebra.CMapDrawer(this, map);
                
                if (map === ColorZebra.colorMap) {
                    map.canvas.setDesaturate(false);
                    $(this).addClass('selected');
                }
                
                map.canvas.draw();
                
                this.title = map.description;
            });
            
            assignActionHandlers();
        });
    }
    
    // Assign all action handlers at startup
    function assignActionHandlers() {
        // Change the active colormap when a thumbnail is clicked
        $('#colormaps>canvas').click(function() {
            var map = ColorZebra.colorMaps[this.id];
            
            if (ColorZebra.colorMap !== map) {
                // Saturate the thumbnail
                map.canvas.setDesaturate(false);
                map.canvas.draw();
                
                // Switch the selected class
                $('#colormaps>.selected').removeClass('selected');
                $(this).addClass('selected');
                
                // Desaturate the current thumbnail
                ColorZebra.colorMap.canvas.setDesaturate(true);
                ColorZebra.colorMap.canvas.draw();
                
                // Switch maps
                ColorZebra.colorMap = map;
                
                // Redraw stuff
                ColorZebra.mainPreview.draw();
                ColorZebra.fixedNumPreview.draw();
            }
        }).hover(function() { // Saturate hovered and focused thumbnails
            saturateThumbnail(this);
        }, function() {
            desaturateThumbnail(this);
        }).focus(function() {
            saturateThumbnail(this);
        }).blur(function() {
            desaturateThumbnail(this);
        }).keydown(function(e) { // React to key events when focused
            var code = e.which; // 13 = Enter, 32 = Space
            if ((code === 13) || (code === 32)) {
                $(this).click();
                return false; // Prevent the event from bubbling further
            }
        });
        
        function saturateThumbnail(thumbnail) {
            var canvas = ColorZebra.colorMaps[thumbnail.id].canvas;
            canvas.setDesaturate(false);
            canvas.draw();
        }
        
        function desaturateThumbnail(thumbnail) {
            var map = ColorZebra.colorMaps[thumbnail.id];
            
            if (ColorZebra.colorMap !== map) {
                map.canvas.setDesaturate(true);
                map.canvas.draw();
            }
        }
        
        // Update the number of colors
        $('#numcolors').keydown(function(e) {
            if (e.which === 13) {
                updateNumColors();
            }
        });
        
        $('#fixednum-apply').click(function() {
            updateNumColors();
        });
        
        function updateNumColors() {
            ColorZebra.numColors = $('#numcolors').val();
            ColorZebra.fixedNumPreview.draw();
        }
        
        // Make the download link work
        $('#download').click(function() {
            switch ($("#format").val()) {
                case 'csv-int':
                    download(this, 'csv', 'csv', ColorZebra.exportIntegerCSV());
                    break;
                case 'csv-float':
                    download(this, 'csv', 'csv', ColorZebra.exportFloatCSV());
                    break;
                case 'ipe':
                    download(this, 'plain', 'isy', ColorZebra.exportIPE());
                    break;
            } 
        });
        
        function download(link, mimeType, extension, fileContents) {
            // Based on "download.js" v4.0, by dandavis; 2008-2015. [CCBY] see http://danml.com/download.html
            if (navigator.msSaveBlob) { // IE10 can't do a[download], only Blobs
                var blob = new Blob([fileContents], {type: 'text/' + mimeType});
                navigator.msSaveBlob(blob, 'colormap.' + extension);
            } else if ('download' in link) { // HTML5 a[download]
                link.href = 'data:text/' + mimeType + ';charset=utf-8,' + encodeURIComponent(fileContents);
                link.download = 'colormap.' + extension;
            } else if (typeof safari !== undefined) { // Handle non-a[download] safari as best we can:
				var url = 'data:application/octet-stream;charset=utf-8,' + encodeURIComponent(fileContents);
                
				if (!window.open(url)) { // Popup blocked, offer direct download:
					if (confirm('Displaying New Document\n\nUse Save As... to download, then click back to return to this page.')) { 
                        location.href=url;
                    }
				}
			} else { // None of these download options are supported. Just show the text and allow them to copy it.
                
            }
        }
        
        // Make our canvases respond to window resizing
        $(window).resize(function() {
            ColorZebra.mainPreview.maximize();
            ColorZebra.mainPreview.draw();
            ColorZebra.fixedNumPreview.maximize();
            ColorZebra.fixedNumPreview.draw();
        });
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));