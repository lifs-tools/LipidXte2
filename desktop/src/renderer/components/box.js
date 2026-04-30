
// in custom-plotly.js
var Plotly = require('plotly.js/lib/core')

// Load in the trace types for box
Plotly.register([
  require('plotly.js/lib/box'),
  require('plotly.js/lib/bar')
])

module.exports = Plotly
