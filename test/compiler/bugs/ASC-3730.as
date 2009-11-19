class main {
    private static const _DAYS_MONTH:Vector.<int> = Vector.<int>([0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]);
    private static const _DAYS_MONTHLEAP:Vector.<int> = Vector.<int>([0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]);
    
    public function main():void {
        var v:int = (true ? _DAYS_MONTHLEAP : _DAYS_MONTH)[4];
        trace(v);
    }
}

var test = new main();
