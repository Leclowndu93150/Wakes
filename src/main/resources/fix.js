function initializeCoreMod() {
    return {
        'remove_onlyin': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.client.server.LanServerPinger'
            },
            'transformer': function(classNode) {
                var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var ArrayList = Java.type('java.util.ArrayList');

                if (classNode.visibleAnnotations != null) {
                    var newAnnotations = new ArrayList();

                    for (var i = 0; i < classNode.visibleAnnotations.size(); i++) {
                        var annotation = classNode.visibleAnnotations.get(i);
                        if (!annotation.desc.equals('Lnet/minecraftforge/api/distmarker/OnlyIn;')) {
                            newAnnotations.add(annotation);
                        }
                    }

                    classNode.visibleAnnotations = newAnnotations;
                }

                return classNode;
            }
        }
    }
}