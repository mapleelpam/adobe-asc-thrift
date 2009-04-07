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

// 1049 - Illegal assignment to a variable specified as constant.
class MyClass2 {
  public static const V1:Vector.<int> = new <int>[5,6,7,8];
}

MyClass2.V1 = new<int>[5,6,7,8];

//1061 - Call to a possibly undefined method _ through a reference with static type _
var j = new <int>[0,3,4].nonexistantMethod();

// 1067 - Implicit coercion of a value of type _ to an unrelated type _.
var v1:Vector.<int> = new <int>['asdf'];
var v2:Vector.<String> = new<String>[new MyClass()];

// 1119 - Access of possibly undefined property _ through a reference with static type _
var i = new <int>[0,1,2].nonexistantproperty;

// 1046: Type was not found or was not a compile-time constant: Vector.
var v4 = new <NotADefinedClass> [4,5];

// 1176 - Comparison between a value with static type _ and a possibly unrelated type _
if (new <Number>[4,5,6][1] == null) {}

var v3:Vector.<Number> = new <Number>[4,5,6];
if (v3[2] == null) {}

// 1189 - Attempt to delete the fixed property _.  Only dynamically defined properties can be deleted.
delete new <int>[1,2,3].fixed


