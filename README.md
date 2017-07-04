

##osumania-score-viewer

This program allows you to visualize osumania scores in a table that can be filtered.
It reads the scores from the scores.db file in your osu directory,
it also reads the osu!.db file to display beatmap information.

### how to use

* make sure you have java 7 or higher: https://java.com
* download the program here: 
* open the program, click "open DB" and select your osu folder.
* click on the columns to sort
* type in filters on the textbox under each column
* the table updates automatically as you play (osu updates the files every 10 min or so,\
the program updates the table every time the file updates.)

### filters

There are 3 types of columns, for text you can just type what you are looking for,
for numbers you can use operators (>, <, =), for example "> 4" to search "all above 4".
Dates are similar to numbers, "<1/1/2017" searches for scores older than 2017.

### License

MIT License. see LICENSE file.
