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
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));