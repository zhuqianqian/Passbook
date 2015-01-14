#PwdBook - A better password tool#

PwdBook runs on Android 4.0+  and helps you to manage sensitive data securely with delightful Material experience[<sup>1</sup>](#note1) and powerful features.

---

## Features

* [AES](http://en.wikipedia.org/wiki/Advanced_Encryption_Standard) encryption, with 256 bits key and [PKCS#5](http://en.wikipedia.org/wiki/PBKDF2) standard
* Data synchronization[<sup>2</sup>](#note2)
* Data import and export
* Password generation
* Multiple Material themes
* Free edit: add or remove any information as you wish, without a template
* In-app Search (under development, available in 2.0)

### About the Name
PwdBook was originally called Passbook. After I realized many other apps also use this name, I decided to change it to PwdBook, another abbreviation form of "Password Book". For all releases before version 2.0, the name was Passbook.

### Download

[![image]](https://play.google.com/store/apps/details?id=com.z299studio.pbfree)
[image]: https://developer.android.com/images/brand/en_generic_rgb_wo_60.png "Get it on Google Play"

---

## Credits

### Icons

* [Material Design Icons](https://github.com/google/material-design-icons) (PwdBook 2.0+), Copyright © Google
* [Icon pack for Android L from Icons8](http://icons8.com/android-L/) (Passbook 1.0+), Copyright © Icons8.com

### Open Source Projects/Libraries

* [OpenCSV](http://opencsv.sourceforge.net), Copyright © OpenCSV team

## License

PwdBook is licensed under [Apache License 2.0](LICENSE).

    /*
     * Copyright (C) 2015 Qianqian Zhu <zhuqianqian.299@gmail.com>
     *
     * This program is free software: you can redistribute it and/or modify
     * it under the terms of the GNU General Public License as published by
     * the Free Software Foundation, either version 3 of the License, or
     * (at your option) any later version.
     *
     * This program is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     * GNU General Public License for more details.
     *
     * You should have received a copy of the GNU General Public License
     * along with this program.  If not, see <http://www.gnu.org/licenses/>.
     */
---

### Notes

<a name="note1" id="md_anchor"><sup>1</sup>Material Themes</a>: Certain Material features require Android 4.4 or Android 5.0.

<a name="note2" id="md_anchor"><sup>2</sup>data synchronization</a>: Synchronization before version 2 uses Google Play Games snapshot service, which can save up to 3MB data. The integration with Google Drive is underdevelopment and it is to be available in version 2.0.