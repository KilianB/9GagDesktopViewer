# 9 Gag Desktop Viewer with repost detection

Seeing the same content is annoying. 9 Gag and Reddit suffer from posts being re-uploaded constantly. 
Wouldn't it be great if we can remember what kind of pictures we have already seen and simply not show
us the same stuff again? 

__Planned features__
*	Duplicate / Report Detection 
*	Offline Mode
*	Jump to posts from specific date

## How it works

9Gag publishes meta information `https://9gag.com/v1/group-posts/group/default/type/SECTION_TITLE?NEXT_TOKEN` as JSON 
which can easily be parsed and point towards the raw image/video file to download the content.

Image duplicates will be detected by utilizing my <a href="https://github.com/KilianB/JImageHash">perceptual image hash library</a>

I have not made any attempts to target videos and gifs yet. As far as I am aware gif and video reposts usually upload the same content
1:1 therefore a naive approach like checking the file size and hashing the first keyframe might already be enough to check for reposts.
If this is not enough maybe implement <a href="https://www.researchgate.net/publication/271547861_Perceptual_video_hashing_based_on_the_Achlioptas's_random_projections">Perceptual video hashing based on the Achlioptas's random projections
</a>.


## Work in progress repository!

![wipgui](https://user-images.githubusercontent.com/9025925/44178150-6103c680-a0f1-11e8-84e1-42f38483c636.png)


Pre-Alpha! This repository contains code pushed at the end of a day, containing multiple debug statements,
half written classes and might not even compile without errors. Currently I am merging an older project into this 
repository. Since I am getting used to datafx and javafx the section is especially messy. Tinkering around a bit here and there.

## Some stats in the meantime

I used some of the legacy code I am reusing in this project to pull some statistics from 9gag a while ago. Here are a few pretty
pictures to look at while the code is coming along eventually. 

![overview](https://user-images.githubusercontent.com/9025925/44178921-da50e880-a0f4-11e8-91f5-5e22d6746bd9.png)
![contenttype](https://user-images.githubusercontent.com/9025925/44178929-e0df6000-a0f4-11e8-8829-fa7a993d8b28.png)
![upvotehot](https://user-images.githubusercontent.com/9025925/44178944-eb015e80-a0f4-11e8-891c-10eaca6f0e8a.png)
![upvotetrending](https://user-images.githubusercontent.com/9025925/44178945-eb015e80-a0f4-11e8-8592-f4b3898bb594.png)
![upvotefresh](https://user-images.githubusercontent.com/9025925/44178943-ea68c800-a0f4-11e8-8928-4272391e1d07.png)

![freshcor](https://user-images.githubusercontent.com/9025925/44178946-eb015e80-a0f4-11e8-8054-ee375afb55d8.png)
![trendingcorrelation](https://user-images.githubusercontent.com/9025925/44178950-eb015e80-a0f4-11e8-91f6-84b6dedb9a3c.png)
![hotcor](https://user-images.githubusercontent.com/9025925/44178947-eb015e80-a0f4-11e8-8966-239bf15d27c9.png)