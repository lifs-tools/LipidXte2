// Partial plotly.js bundle: cartesian core plus the two trace types the pages use.
import Plotly from 'plotly.js/lib/core'
import box from 'plotly.js/lib/box'
import bar from 'plotly.js/lib/bar'

Plotly.register([box, bar])

// plotly.js 4 shows an "Upload to Cloud" modebar button (cloud.plotly.com) by default;
// the plots stay on this server, so leave it out of the modebar.
Plotly.setPlotConfig({ showSendToCloud: false })

export default Plotly
