# CoronaMelder Android - Coronavirus dashboard feature

## Introduction
The Coronavirus dashboard feature shows data for the following topics similar to [coronadashboard.rijksoverheid.nl](https://coronadashboard.rijksoverheid.nl/):

- People tested positive
- People using CoronaMelder
- People with coronavirus on IC
- Vaccination rate
- Fully vaccinated (18+)

For each topic a card is shown on the status screen. More information can be found on the detail page when selecting a card. The cards are replaced by a menu item when status screen has important information to show like an exposure or when the framework is disabled.
It's possible to disable this feature from the in-app settings screen.

## Status
This feature is currently stored on a feature branch while CoronaMelder has been deactivated. None of the coronavirus dashboard related code has been merged to main.

### TODO Android: Accessibility
An accessibility check has been done and the following issues are still open:

- 2.1.1 - Possibility to navigate through the graph when focused similar to the drag gesture.
- 2.4.1 - Unique titles for each topic detail screen
- 1.4.3/1.4.11 - Graph label/grid lines contrast isn't high enough

These should be fixed before releasing to ensure we keep the WCAG 2.1 AA status.

## Protocol
The following enpoint can be used to fetch dashboard data:

GET `v1/dashboarddata`

Response:

```
{
"positiveTestResults": {
	"infectedPercentage": 66.2,
	"infectedMovingAverage": {
	"timestampStart": 1654560000,
	"timestampEnd": 1655164800,
	"value": 2218
	},
	"sortingValue": 0,
	"values": [
	{
		"timestamp": 1652227200,
		"value": 1403
	},
	{
		"timestamp": 1652313600,
		"value": 1448
	},
	..
	],
	"highlightedValue": {
	"timestamp": 1655164800,
	"value": 2887
	},
	"moreInfoUrl": "https://coronadashboard.rijksoverheid.nl/landelijk/positief-geteste-mensen"
},
"coronaMelderUsers": {
	"sortingValue": 1,
	"values": [
	{
		"timestamp": 1652220000,
		"value": 2336200
	},
	{
		"timestamp": 1652306400,
		"value": 2338400
	},
	..
	],
	"highlightedValue": {
	"timestamp": 1653256800,
	"value": 2349278
	},
	"moreInfoUrl": "https://coronadashboard.rijksoverheid.nl/landelijk/coronamelder"
},
"hospitalAdmissions": {
	"hospitalAdmissionMovingAverage": {
	"timestampStart": 1654214400,
	"timestampEnd": 1654732800,
	"value": 33
	},
	"sortingValue": 2,
	"values": [
	{
		"timestamp": 1652227200,
		"value": 43
	},
	{
		"timestamp": 1652313600,
		"value": 44
	},
	..
	],
	"highlightedValue": {
	"timestamp": 1655078400,
	"value": 121
	},
	"moreInfoUrl": "https://coronadashboard.rijksoverheid.nl/landelijk/ziekenhuis-opnames"
},
"icuAdmissions": {
	"icuAdmissionMovingAverage": {
	"timestampStart": 1654214400,
	"timestampEnd": 1654819200,
	"value": 3
	},
	"sortingValue": 3,
	"values": [
	{
		"timestamp": 1654819200,
		"value": 3
	},
	{
		"timestamp": 1654732800,
		"value": 4
	},
	..
	],
	"highlightedValue": {
	"timestamp": 1655078400,
	"value": 3
	},
	"moreInfoUrl": "https://coronadashboard.rijksoverheid.nl/landelijk/intensive-care-opnames"
},
"vaccinationCoverage": {
	"vaccinationCoverage18Plus": 83.2,
	"boosterCoverage18Plus": 64,
	"sortingValue": 4,
	"values": null,
	"highlightedValue": null,
	"moreInfoUrl": "https://coronadashboard.rijksoverheid.nl/landelijk/vaccinaties"
},
"moreInfoUrl": "https://coronadashboard.rijksoverheid.nl"
}
```
