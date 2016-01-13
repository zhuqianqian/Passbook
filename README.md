#Passbook - A better password tool#
[![Build Status][travis-image]][travis-url]

Passbook runs on Android 4.0+  and helps you to manage sensitive data securely with delightful Material experience[<sup>1</sup>](#note1) and powerful features.

---

## Features

* [AES](http://en.wikipedia.org/wiki/Advanced_Encryption_Standard) encryption, with 256 bits key and [PKCS#5](http://en.wikipedia.org/wiki/PBKDF2) standard
* Data synchronization[<sup>2</sup>](#note2)
* Data import and export
* Password generation
* Multiple Material themes
* Free edit: add or remove any information as you wish, without a template
* In-app Search

### Download

[![image]](https://play.google.com/store/apps/details?id=com.z299studio.pbfree)
[image]: https://developer.android.com/images/brand/en_generic_rgb_wo_60.png "Get it on Google Play"

---

## Credits

### Translations

* Français: Xuechen Xu
* Español: [oseliko7](https://github.com/joseliko7).
* Many thanks to you guys for the translations!

* Please visit [here](https://299studio.oneskyapp.com/collaboration/project?id=39783) to help on translations.
* Many thanks to [OneSky](http://www.oneskyapp.com) for providing the translation collaboration platform.

### Icons

* [Material Design Icons](https://github.com/google/material-design-icons) (Passbook 2.0+), Copyright © Google
* [Icon pack for Android L from Icons8](http://icons8.com/android-L/) (Passbook 1.0+), Copyright © Icons8.com

### Open Source Projects/Libraries

* [OpenCSV](http://opencsv.sourceforge.net), Copyright © OpenCSV team

## License

Passbook is licensed under [Apache License 2.0](LICENSE).

    /*
     * Copyright (C) 2015 Qianqian Zhu <zhuqianqian.299@gmail.com>
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
---

### Notes

<a name="note1" id="md_anchor"><sup>1</sup>Material Themes</a>: Certain Material features require Android 4.4 or Android 5.0.

<a name="note2" id="md_anchor"><sup>2</sup>data synchronization</a>: Synchronization before version 2 uses Google Play Games snapshot service, which can save up to 3MB data. Since version 2, you can choose from Google Play Games snapshot service or Google Drive as the server to synchronize your data. Even if you chose Google Play Games service, the data is actually stored in your Google Drive folder, managed by Google Play Games service, and it can only be accessed by Passbook.

[travis-url]: https://travis-ci.org/zhuqianqian/Passbook 
[travis-image]: https://travis-ci.org/zhuqianqian/Passbook.svg?branch=master
