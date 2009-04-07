/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is [Open Source Virtual Machine.].
 *
 * The Initial Developer of the Original Code is
 * Adobe System Incorporated.
 * Portions created by the Initial Developer are Copyright (C) 2007-2008
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Adobe AS3 Team
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

class MyClass {}


// 1008 - Missing type declaration.
var v1 = new <int> [67,8,9];

// 1086 - ActionScript 3.0 iterates over an object's properties within a "for x in target" statement in random order
for (i in new <int> [3,4,5]) {}

// 1092 - Negative value used where a uint (non-negative) value is expected.
var v2:Vector.<uint> = new <uint>[1,2,-3,4];

// 1098 - Illogical comparison with NaN.
if (n == NaN) {}
if (v3[2] == NaN) {}
if (new <Number>[4,5,6][1] == NaN) {}


//1102 - Impossible null assignment.
var b1:Boolean = null;
var v4:Vector.<Boolean> = new <Boolean> [null,true];
var v5:Vector.<uint> = new <uint> [3,null,4];
var v6:Vector.<int> = new <int> [9,null];
var v7:Vector.<Number> = new <Number> [98.3,null];

// 3590 - Non-Boolean value used where a Boolean value was expected.
var b2:Boolean = "hello";
var v8:Vector.<Boolean> = new <Boolean> [true, true, "hello"];

var b3:Boolean = new MyClass();
var v9:Vector.<Boolean> = new <Boolean> [new MyClass()];

// 3600 - Possible attempt to delete a fixed property.
delete new <int>[1,2,3].fixed
delete new <String>['r'].length

