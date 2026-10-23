// Partial plotly.js bundle: cartesian core plus the two trace types the pages use.
import Plotly from 'plotly.js/lib/core'
import box from 'plotly.js/lib/box'
import bar from 'plotly.js/lib/bar'

Plotly.register([box, bar])

export default Plotly
