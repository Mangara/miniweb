(function( ColorZebra, $, undefined ) {
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
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));