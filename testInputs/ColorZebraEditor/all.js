(function( ColorZebra, $, undefined ) {
    ColorZebra.Settings = function(inverted) {
        this.inverted = inverted;
    }
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
    
    ColorZebra.Color.LCHtoLAB = function(lch) {
        var theta = Math.PI * lch[2] / 180;
        return [lch[0], lch[1] * Math.cos(theta), lch[1] * Math.sin(theta)]
    }
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

        this.getControlPoints = function() {
            return points;
        }

        this.getSpline = function() {
            return spline;
        }
        
        function normalizeLightness(value) {
            var dLightness = Math.max(0, Math.min(1, value)) * (points[points.length - 1][0] - points[0][0]);
            return (ColorZebra.settings.inverted ? points[points.length - 1][0] - dLightness : points[0][0] + dLightness);
        }
    }
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
            [ ColorZebra.Color.LCHtoLAB([10, 78, -60]),  
              ColorZebra.Color.LCHtoLAB([20, 100, -60]),
              ColorZebra.Color.LCHtoLAB([30, 78, -40]),
              ColorZebra.Color.LCHtoLAB([40, 74, -20]),                
              ColorZebra.Color.LCHtoLAB([50, 80, 0]),                 
              ColorZebra.Color.LCHtoLAB([60, 80, 20]),
              ColorZebra.Color.LCHtoLAB([70, 72, 50]),
              ColorZebra.Color.LCHtoLAB([80, 84, 77]),
              ColorZebra.Color.LCHtoLAB([95, 90, 95]) ],
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
            [ ColorZebra.Color.LCHtoLAB([60, 20, 180]),
              ColorZebra.Color.LCHtoLAB([65, 30, 135]),
              ColorZebra.Color.LCHtoLAB([70, 35, 75]),
              ColorZebra.Color.LCHtoLAB([75, 45, 85]),
              ColorZebra.Color.LCHtoLAB([80, 22, 90]), 
              [85, 0, 0] ],
            3
         ),
        
        'Dawn' : new ColorZebra.ColorMap(
            'Dawn',
            'A more saturated geographical colour map, best used with relief shading',
            [ ColorZebra.Color.LCHtoLAB([65, 50, 135]),
              ColorZebra.Color.LCHtoLAB([75, 45, 75]),
              ColorZebra.Color.LCHtoLAB([80, 45, 85]),
              ColorZebra.Color.LCHtoLAB([85, 22, 90]),        
              [90, 0, 0] ],
            3
         ),
         
        'Water' : new ColorZebra.ColorMap(
            'Water',
            'A water depth colour map',
            [ ColorZebra.Color.LCHtoLAB([50, 35, -95]),
              ColorZebra.Color.LCHtoLAB([60, 25, -95]),
              ColorZebra.Color.LCHtoLAB([70, 25, -95]),
              ColorZebra.Color.LCHtoLAB([80, 20, -95]),
              [95, 0, 0] ],
            3
         )
    }
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
    ColorZebra.Preview = function(theCanvas) {
        var canvas = theCanvas;
        
        this.maximize = function() {
            var parent = $(canvas).parent();

            if (canvas.width != parent.width() || canvas.height != parent.height() - 4) {
                canvas.width = parent.width();
                canvas.height = parent.height() - 4; // No clue why the -4 is necessary.
                computeValues();
            }
        }
        
        this.draw = function() {
            if (canvas === undefined) {
                alert('Canvas undefined');
                return;
            }
            
            drawPiecewiseLinear();
        }

        var STEPS = 10;
        var values, stops;

        function computeValues() {
            var x, y, width = canvas.width;

            values = [];
            stops = [];

            var amp = [];
            for (y = STEPS; y > 0; y--) {
                var yt = y / STEPS;
                amp[y] = 0.05 * yt * yt;

                stops[y] = 1 - y / STEPS;
            }

            var sinVal = [];
            for (x = 0; x < 8; x++) {
                sinVal.push(Math.sin(x * Math.PI / 4));
            }
                        
            for (x = 0; x < width; x++) {
                var xt = x / (width - 1);
                values[x] = [];

                for (y = STEPS; y > 0; y--) {
                    values[x][y] = amp[y] * sinVal[x % 8] + getRamp(xt, amp[y]);
                }
            }
        }

        function drawPiecewiseLinear() {
            // The test image consists of a sine wave plus a ramp function
            // The sine wave has a wavelength of 8 pixels (which is why we multiply by 2pi/8 = pi/4)
            // The base sine wave has amplitude 0.05, so that it spans 10% of the value range
            // In each column, the amplitude of the sine wave ranges from 0 at the bottom to 0.05 at the top, increasing quadratically
            // In each row, the ramp goes from <z> on the left to (1 - <z>) on the right, where <z> = 0.05 * ((height - y) / height)^2 is the maximum amplitude of the sine wave in that row
            // Drawing it per-pixel is slow, because context fillstyle changes are very expensive (much more than any calculations we're doing).
            // This method draws the same image, except that the amplitude of the sine wave approximates the quadratic modulation with a piecewise linear one.
            // For STEPS = 10 this is nearly impossible to distinguish visually.
            var context = canvas.getContext("2d"),
                width = canvas.width,
                height = canvas.height;
                        
            for (var x = 0; x < width; x++) {
                var my_gradient = context.createLinearGradient(0, 0, 0, height);
                
                for (var y = STEPS; y > 0; y--) {
                    my_gradient.addColorStop(stops[y], ColorZebra.colorMap.getCSSColor(values[x][y]));
                }
                
                context.fillStyle = my_gradient;
                context.fillRect(x, 0, 1, height);
            }
        }

        function drawQuadratic() {
            // Slightly slower way of drawing the test image pixel-perfect
            var context = canvas.getContext("2d"),
                x, y,
                width = canvas.width,
                height = canvas.height;

            var imageData = context.createImageData(width, height);

            var amp = [];
            for (y = 0; y < height; y++) {
                var yt = (height - y) / (height - 1);
                amp.push(0.05 * yt * yt);
            }

            var sinVal = [];
            for (x = 0; x < 8; x++) {
                sinVal.push(Math.sin(x * Math.PI / 4));
            }

            for (x = 0; x < width; x++) {
                var xt = x / (width - 1); // x mapped to [0, 1]
                
                for (y = 0; y < height; y++) {
                    var val = amp[y] * sinVal[x % 8] + getRamp(xt, amp[y]);
                    var rgb = ColorZebra.Color.LABtoIntegerRGB(ColorZebra.colorMap.getLABColor(val));

                    var pixel = (y * width + x) * 4;
                    imageData.data[pixel    ] = rgb[0];
                    imageData.data[pixel + 1] = rgb[1];
                    imageData.data[pixel + 2] = rgb[2];
                    imageData.data[pixel + 3] = 255; // opaque
                }
            }

            context.putImageData(imageData, 0, 0);
        }
        
        function getRamp(xt, amp) {
            return amp + (1 - 2 * amp) * xt;
        }
    }
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
    // Important variables with their initial values
    ColorZebra.colorMap = ColorZebra.colorMaps['Lake'];
    ColorZebra.numColors = 12;
    
    // Handle on-load stuff
    $(document).ready(function() {
        // Init settings
        ColorZebra.settings = new ColorZebra.Settings(false);

        // Prepare our preview panels
        ColorZebra.mainPreview = new ColorZebra.Preview($('#preview')[0]);
        ColorZebra.mainPreview.maximize();
        ColorZebra.mainPreview.draw();
        
        ColorZebra.fixedNumPreview = new ColorZebra.FixedNumPreview($('#fixednum-preview')[0]);
        ColorZebra.fixedNumPreview.maximize();
        ColorZebra.fixedNumPreview.draw();
        
        // Create all thumbnails
        $('#colormaps>button').each(function() {
            var map = ColorZebra.colorMaps[this.id];
            
            if (map === ColorZebra.colorMap) {
                $(this).addClass('selected');
            }
            
            var canvas = document.createElement("canvas");
            var width = Math.ceil($(this).outerWidth());
            canvas.width = width;
            canvas.height = 1;

            if (canvas.getContext) {
                var ctx = canvas.getContext('2d');
                var imageData = ctx.createImageData(width, 1);
                
                for (var x = 0; x < width; x++) {
                    var val = x / (width - 1);
                    var rgb = ColorZebra.Color.LABtoIntegerRGB(map.getLABColor(val));
                    var pixel = x * 4;
                    imageData.data[pixel    ] = rgb[0];
                    imageData.data[pixel + 1] = rgb[1];
                    imageData.data[pixel + 2] = rgb[2];
                    imageData.data[pixel + 3] = 255; // opaque
                }

                ctx.putImageData(imageData, 0, 0);

                this.style.backgroundImage = "url(" + canvas.toDataURL() + ")";
            }
            
            this.title = map.description;
        });
        
        createControlPointWidgets();
        
        assignActionHandlers();
    });
    
    // Assign all action handlers at startup
    function assignActionHandlers() {
        // Make our canvases respond to window resizing
        $(window).resize(function() {
            ColorZebra.mainPreview.maximize();
            ColorZebra.mainPreview.draw();
            ColorZebra.fixedNumPreview.maximize();
            ColorZebra.fixedNumPreview.draw();
        });

        // Change the active colormap when a thumbnail is clicked
        $('#colormaps>button').click(function() {
            var map = ColorZebra.colorMaps[this.id];
            
            if (ColorZebra.colorMap !== map) {
                // Switch the selected class
                deselectColormap();
                $(this).addClass('selected');
                
                // Switch maps
                setColorMap(map);
            }
        });
        
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
            switch ($('#format').val()) {
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

        $('#settings-toggle').click(function() {
            $('#settings').slideToggle(500);
            $('#settings-toggle>i').html(
                $('#settings-toggle>i').html() === 'expand_more' ? 'expand_less' : 'expand_more'
            );
        });

        $('#invert').click(function() {
            ColorZebra.settings.inverted = !ColorZebra.settings.inverted;

            // Redraw stuff
            ColorZebra.mainPreview.draw();
            ColorZebra.fixedNumPreview.draw();

            $('#colormaps>button').toggleClass('inverted');
        });

        // Editor controls
        $('#remove').click(function() {
            var selectedWidget = getSelectedWidget();

            // Update min of neighbours
            var myLightnessInput = selectedWidget.children("input[type=number]").first();
            var myMin = myLightnessInput.attr('min');
            var myMax = myLightnessInput.attr('max');
            selectedWidget.prev().children("input[type=number]").first().attr('max', myMax);
            selectedWidget.next().children("input[type=number]").first().attr('min', myMin);

            var nextWidget = (selectedWidget.next().length ? selectedWidget.next() : selectedWidget.prev());
            selectedWidget.remove();
            selectWidget(nextWidget);

            updateColorMapFromEditor();
        });

        $('#insert-before').click(function() {
            var selectedWidget = getSelectedWidget();
            var color = getWidgetColor(selectedWidget);
            color[0]--;

            var min = (selectedWidget.prev().length ? getWidgetLightness(selectedWidget.prev()) + 1 : 0);
            var max = color[0];

            selectedWidget.prev().children("input[type=number]").first().attr('max', color[0] - 1);
            selectedWidget.children("input[type=number]").first().attr('min', color[0] + 1);

            var newWidget = createWidget(min, max);
            syncWidget(newWidget, color);

            selectedWidget.before(newWidget);

            selectWidget(newWidget);

            updateColorMapFromEditor();
        });

        $('#insert-after').click(function() {
            var selectedWidget = getSelectedWidget();
            var color = getWidgetColor(selectedWidget);
            color[0]++;

            var min = color[0];
            var max = (selectedWidget.next().length ? getWidgetLightness(selectedWidget.next()) - 1 : 0);

            selectedWidget.children("input[type=number]").first().attr('max', color[0] - 1);
            selectedWidget.next().children("input[type=number]").first().attr('min', color[0] + 1);

            var newWidget = createWidget(min, max);
            syncWidget(newWidget, color);

            selectedWidget.after(newWidget);

            selectWidget(newWidget);

            updateColorMapFromEditor();
        });

        $('#lightness').on('input', function() {
            setWidgetLightness(getSelectedWidget(), this.value);
        });

        var abDrag = false;
        $('#abControl').mousedown(function(event) {
            abDrag = true;
            changeAB(getFractionalClickCoordinates(this, event));
        }).mousemove(function(event) {
            if (abDrag) {
                changeAB(getFractionalClickCoordinates(this, event));
            }
        }).mouseup(function(event) {
            abDrag = false;
        });

        function getFractionalClickCoordinates(element, event) {
            var offsetX = 0, offsetY = 0;
            var el = element;

            while (el.offsetParent) {
                offsetX += el.offsetLeft;
                offsetY += el.offsetTop;
                el = el.offsetParent;
            }

            var x = (event.pageX - offsetX) / element.scrollWidth;
            var y = (event.pageY - offsetY) / element.scrollHeight;

            return [x, y];
        }

        function changeAB(coords) {
            var a = Math.round(minAB + coords[0] * (maxAB - minAB));
            var b = Math.round(minAB + coords[1] * (maxAB - minAB));

            setWidgetAB(getSelectedWidget(), a, b);
        }
    }
    
    function deselectColormap() {
        $('#colormaps>.selected').removeClass('selected');
    }
    
    function createControlPointWidgets() {
        var points = ColorZebra.colorMap.getControlPoints();

        for (var i = 0, max = points.length; i < max; i++) {
            var minL = (i === 0 ? 0 : points[i - 1][0] + 1);
            var maxL = (i === points.length - 1 ? 100 : points[i + 1][0] - 1);
            var widget = createWidget(minL, maxL);
            syncWidget(widget, points[i]);
            $("#cp-widgets").append(widget);
        }

        selectWidget($('#cp-widgets').children().first());
    }
    
    function createWidget(minL, maxL) {
        var widget = $("<div class=control-point>" + 
            "<input type=number min=" + minL + " max=" + maxL + "> " +
            "<input type=number min=-128 max=128> " + 
            "<input type=number min=-128 max=128>" + 
            "</div>");
        
        // Update selection
        widget.click(function() {
            selectWidget($(this));
        });
        
        // React to changes in the control points
        widget.children('input[type="number"]').change(function() {
            var widget = $(this).parent(); // TODO: is this necessary?

            updateWidgetBackground(widget);
            updateColorMapFromEditor();

            if ($(this).is(':first-child')) {
                // The lightness changed
                updateLightnessControls();
                updateButtonsEnabledState();

                var newVal = parseInt($(this).val());

                widget.prev().children("input[type=number]").first().attr('max', newVal - 1);
                widget.next().children("input[type=number]").first().attr('min', newVal + 1);
            } else {
                updateABControls();
            }
        });
        
        return widget;
    }
    
    function syncWidget(widget, point) {
        var labTextfields = $(widget).children("input[type=number]");

        labTextfields[0].value = point[0];
        labTextfields[1].value = point[1];
        labTextfields[2].value = point[2];

        updateWidgetBackground(widget);
    }
    
    function updateWidgetBackground(widget) {
        $(widget).css("background-color", ColorZebra.Color.LABtoCSS(getWidgetColor(widget)));
    }
    
    function getWidgetColor(widget) {
        return $(widget).children("input[type=number]").get().map( function(x) { return parseInt(x.value); } );
    }
    
    function setWidgetAB(widget, a, b) {
        var labTextfields = $(widget).children("input[type=number]");
        var changed = false;

        if (labTextfields.eq(1).val() != a) {
            changed = true;
            labTextfields.eq(1).val(a);
        }

        if (labTextfields.eq(2).val() != b) {
            changed = true;
            labTextfields.eq(2).val(b);
        }

        if (changed) {
            labTextfields.eq(1).trigger('change');
        }
    }

    function getWidgetLightness(widget) {
        return parseInt($(widget).children("input[type=number]")[0].value);
    }
    
    function setWidgetLightness(widget, lightness) {
        var lightnessInput = $(widget).children("input[type=number]").first();

        if (lightnessInput.val() != lightness) {
            lightnessInput.val(lightness);
            lightnessInput.trigger('change');
        }
    }

    function getSelectedWidget() {
        return $('#cp-widgets>.selected').first();
    }
    
    function selectWidget(widget) {
        getSelectedWidget().removeClass('selected');
        widget.addClass('selected');

        updateColorControls();
        updateButtonsEnabledState();
    }
    
    function updateColorControls() {
        updateLightnessControls();
    }

    function updateLightnessControls() {
        var selectedWidget = getSelectedWidget();
        var color = getWidgetColor(selectedWidget);
        
        updateLightnessSlider(selectedWidget, color);

        var bgCanvas = $('#abBackground')[0];
        drawColorCanvasBackground(bgCanvas.getContext("2d"), color[0], bgCanvas.width, bgCanvas.height);

        updateABControls();
    }

    function updateABControls() {
        var color = getWidgetColor(getSelectedWidget());
        var canvas = $('#abControl')[0];
        var context = canvas.getContext("2d"),
            width = canvas.width,
            height = canvas.height;

        context.clearRect(0, 0, width, height);
        drawColorCanvasCurve(context, color, width, height);
        drawColorCanvasIndicator(context, color, width, height);
    }
    
    function updateLightnessSlider(selectedWidget, color) {
        var lightnessInput = selectedWidget.children("input[type=number]").first();
        var min = parseInt(lightnessInput.attr('min'));
        var max = parseInt(lightnessInput.attr('max'));
        
        // Update slider values
        var lightnessSlider = $('#lightness').first();
        lightnessSlider.attr('min', min);
        lightnessSlider.attr('max', max);
        lightnessSlider.val(color[0]);
        
        // Update slider background
        var start = 0.5;
        var end = 14.5;
        var nStops = 8;

        var rule = "background: linear-gradient(to right, ";

        for (var i = 0; i < nStops; i++) {
            var f = i / (nStops - 1);
            rule += ColorZebra.Color.LABtoCSS([min + f * (max - min), color[1], color[2]]);
            rule += " " + (start + f * (end - start)) + "em";
            rule += (i == nStops - 1 ? ");" : ", ");
        }

        $("#dynamic").text("#lightness::-webkit-slider-runnable-track { " + rule + " }");
    }
    
    var minAB = -128;
    var maxAB =  128;
    
    function drawColorCanvasBackground(context, lightness, width, height) {
        var imageData = context.createImageData(width, height);

        for (var x = 0; x < width; x++) {
            var xt = x / (width - 1); // x mapped to [0, 1]
            var a = minAB + xt * (maxAB - minAB);
            
            for (var y = 0; y < height; y++) {
                var yt = y / (height - 1);
                var b = minAB + yt * (maxAB - minAB);
                var rgb = ColorZebra.Color.LABtoIntegerRGB([lightness, a, b]);

                var pixel = (y * width + x) * 4;
                imageData.data[pixel    ] = rgb[0];
                imageData.data[pixel + 1] = rgb[1];
                imageData.data[pixel + 2] = rgb[2];
                imageData.data[pixel + 3] = 255; // opaque
            }
        }

        context.putImageData(imageData, 0, 0);
    }
    
    function drawColorCanvasCurve(context, color, width, height) {
        for (var i = 0; i <= 1; i+=0.01) {
            var c = ColorZebra.colorMap.getLABColor(i);
            var x = width * (c[1] - minAB) / (maxAB - minAB);
            var y = height * (c[2] - minAB) / (maxAB - minAB);
            
            drawColorCanvasCircle(context, c, color[0], 0.5, x, y, 4);
        }
    }
    
    function drawColorCanvasIndicator(context, color, width, height) {
        var x = width * (color[1] - minAB) / (maxAB - minAB);
        var y = height * (color[2] - minAB) / (maxAB - minAB);
        
        drawColorCanvasCircle(context, color, color[0], 2, x, y, 10);
    }
    
    function drawColorCanvasCircle(context, fillColor, lightness, thickness, x, y, radius) {
        context.fillStyle = ColorZebra.Color.LABtoCSS(fillColor);
        context.strokeStyle = (lightness < 70 ? 'white' : 'black');
        context.lineWidth = thickness;

        context.beginPath();
        context.arc(x, y, radius, 0, Math.PI * 2, true);
        context.fill();
        context.stroke();
    }
    
    function updateButtonsEnabledState() {
        var selectedWidget = getSelectedWidget();
        var lightnessInput = selectedWidget.children('input[type=number]').first();
        var lightness = parseInt(lightnessInput.val());
        var min = parseInt(lightnessInput.attr('min'));
        var max = parseInt(lightnessInput.attr('max'));

        $('#insert-before').prop("disabled", lightness <= min);
        $('#insert-after').prop("disabled", lightness >= max);
        $('#remove').prop("disabled", $('.control-point').length === 3);
    }
    
    function setColorMap(newMap) {
        ColorZebra.colorMap = newMap;

        ColorZebra.mainPreview.draw();
        ColorZebra.fixedNumPreview.draw();

        $("#cp-widgets").empty();
        createControlPointWidgets(); // Updates color controls and button state
    }
    
    function updateColorMapFromEditor() {
        var points = $('#cp-widgets').children().get().map(getWidgetColor);

        ColorZebra.colorMap = new ColorZebra.ColorMap(
            'Custom',
            'Custom color map',
            points,
            3
        );
        
        deselectColormap();

        ColorZebra.mainPreview.draw();
        ColorZebra.fixedNumPreview.draw();
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));