### Passbook - A better password tool

[![Build Status][travis-image]][travis-url]  [![Dependency Status][dependency-image]][dependency-url]  [![API][api-image]][api-url]

Passbook runs on Android 5.0+  and helps you to manage sensitive data securely with delightful Material experience and powerful features.

---

## Features

* [AES](http://en.wikipedia.org/wiki/Advanced_Encryption_Standard) encryption, with 256 bits key and [PKCS#5](http://en.wikipedia.org/wiki/PBKDF2) standard
* Data synchronization[<sup>1</sup>](#note1)
* Data import and export
* Password generation
* Free edit: add or remove any information as you wish, without a template
* In-app Search
* Fingerprint authentication[<sup>2</sup>](#note2)

### Download

<a href="https://play.google.com/store/apps/details?id=com.z299studio.pbfree"><img src="https://play.google.com/intl/en_us/badges/images/apps/en-play-badge.png" height="60" width="204" ></a>

---

## Credits

### Translations

* Français: Xuechen Xu
* Español: [oseliko7](https://github.com/joseliko7).
* Many thanks to you guys for the translations!

* Please visit [here](https://299studio.oneskyapp.com/collaboration/project?id=39783) to help on translations.
* Many thanks to [OneSky](http://www.oneskyapp.com) for providing the translation collaboration platform.

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

<a name="note1" id="md_anchor"><sup>1</sup>Data Synchronization</a>: Passbook provides two ways of data synchronization: Google Play Games snapshot and Google Drive. Even if you chose Google Play Games service, the data is also actually stored in your Google Drive folder, managed by Google Play Games service, and accessed by Passbook only.

<a name="note2" id="md_anchor"><sup>2</sup>Fingerprint Authentication</a>: Fingerprint authentication requires Android 6 Marshmallow and a working fingerprint sensor on your device. Manufacture specific fingerprint authentication is not supported.

[travis-url]: https://travis-ci.org/zhuqianqian/Passbook 
[travis-image]: https://travis-ci.org/zhuqianqian/Passbook.svg?branch=master

[dependency-url]:https://snyk.io/test/github/zhuqianqian/Passbook?targetFile=app%2Fbuild.gradle
[dependency-image]:https://snyk.io/test/github/zhuqianqian/Passbook/badge.svg?targetFile=app%2Fbuild.gradle

[api-url]:https://android-arsenal.com/api?level=14
[api-image]:https://img.shields.io/badge/API-14%2B-blue.svg?style=flat
