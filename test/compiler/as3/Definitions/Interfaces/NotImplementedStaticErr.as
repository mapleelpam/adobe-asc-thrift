/*
 *  Expected Error
 *  Interface method hello in namespace Errors$internal not implemented by class Errors$internal:MyClass
 *
 *  http://flashqa.macromedia.com/bugapp/detail.asp?ID=136985
 */

package Errors {
	interface IInterface {
		static function hello()
	}
	
	class MyClass implements IInterface {
	
	}

}
	