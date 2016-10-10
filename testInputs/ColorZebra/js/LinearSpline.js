(function( ColorZebra, $, undefined ) {
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
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));