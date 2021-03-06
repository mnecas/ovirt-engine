package org.ovirt.engine.api.restapi.types;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.api.model.Core;
import org.ovirt.engine.api.model.Cores;
import org.ovirt.engine.api.model.Cpu;
import org.ovirt.engine.api.model.NumaNode;
import org.ovirt.engine.api.model.NumaNodePin;
import org.ovirt.engine.api.model.NumaNodePins;
import org.ovirt.engine.api.model.VirtualNumaNode;
import org.ovirt.engine.api.restapi.utils.GuidUtils;
import org.ovirt.engine.core.common.businessentities.VdsNumaNode;
import org.ovirt.engine.core.common.businessentities.VmNumaNode;
import org.ovirt.engine.core.compat.Guid;

public class NumaMapper {

    @Mapping(from = VdsNumaNode.class, to = NumaNode.class)
    public static NumaNode map(VdsNumaNode entity, NumaNode template) {
        NumaNode model = template != null ? template : new NumaNode();
        if (entity.getId() != null) {
            model.setId(entity.getId().toString());
        }
        model.setIndex(entity.getIndex());
        model.setMemory(entity.getMemTotal());
        if (entity.getCpuIds() != null && entity.getCpuIds().size() > 0) {
            Cpu cpu = new Cpu();
            Cores cores = new Cores();
            for (int id : entity.getCpuIds()) {
                Core core = new Core();
                core.setIndex(id);
                cores.getCores().add(core);
            }
            cpu.setCores(cores);
            model.setCpu(cpu);
        }
        if (entity.getNumaNodeDistances() != null && entity.getNumaNodeDistances().size() > 0) {
            model.setNodeDistance(StringUtils.join(entity.getNumaNodeDistances().values(), " "));
        }
        return model;
    }

    @Mapping(from = VmNumaNode.class, to = VirtualNumaNode.class)
    public static VirtualNumaNode map(VmNumaNode entity, VirtualNumaNode template) {
        VirtualNumaNode model = template != null ? template : new VirtualNumaNode();
        if (entity.getId() != null) {
            model.setId(entity.getId().toString());
        }
        model.setIndex(entity.getIndex());
        model.setMemory(entity.getMemTotal());
        if (entity.getCpuIds() != null && entity.getCpuIds().size() > 0) {
            Cpu cpu = new Cpu();
            Cores cores = new Cores();
            for (int id : entity.getCpuIds()) {
                Core core = new Core();
                core.setIndex(id);
                cores.getCores().add(core);
            }
            cpu.setCores(cores);
            model.setCpu(cpu);
        }
        if (entity.getVdsNumaNodeList() != null && entity.getVdsNumaNodeList().size() > 0) {
            NumaNodePins pins = new NumaNodePins();
            for (Integer pinnedIndex : entity.getVdsNumaNodeList()) {
                NumaNodePin pin = new NumaNodePin();
                pin.setIndex(pinnedIndex);
                pins.getNumaNodePins().add(pin);
            }
            model.setNumaNodePins(pins);
        }
        return model;
    }

    @Mapping(from = VirtualNumaNode.class, to = VmNumaNode.class)
    public static VmNumaNode map(VirtualNumaNode model, VmNumaNode template) {
        VmNumaNode entity = template != null ? template : new VmNumaNode();
        if (model.isSetId()) {
            entity.setId(GuidUtils.asGuid(model.getId()));
        }
        if (entity.getId() == null) {
            entity.setId(Guid.newGuid());
        }
        if (model.isSetIndex()) {
            entity.setIndex(model.getIndex());
        }
        Cpu cpu = model.getCpu();
        if (cpu != null) {
            List<Integer> ids = new ArrayList<>();
            Cores cores = cpu.getCores();
            if (cores != null) {
                for (Core core : cores.getCores()) {
                    Integer index = core.getIndex();
                    if (index != null) {
                        ids.add(index);
                    }
                }
            }
            entity.setCpuIds(ids);
        }
        if (model.isSetMemory()) {
            entity.setMemTotal(model.getMemory());
        }
        if (model.isSetNumaNodePins()) {
            entity.setVdsNumaNodeList(model.getNumaNodePins().getNumaNodePins().stream()
                    .map(NumaNodePin::getIndex)
                    .collect(Collectors.toList()));
        }
        return entity;
    }
}
