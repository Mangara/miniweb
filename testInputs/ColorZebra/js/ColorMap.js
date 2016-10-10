(function( ColorZebra, $, undefined ) {
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
            return points[0][0] + value * (points[points.length - 1][0] - points[0][0]);
        }
    }
}( window.ColorZebra = window.ColorZebra || {}, jQuery ));