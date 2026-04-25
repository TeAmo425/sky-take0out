package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceimpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish、dish_flavor
     *
     * @param dishDTO
     */


    @Transactional

    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //向菜品表插入数据
        dishMapper.insert(dish);
        //获取insert语句生成的主键值
        Long dishId = dish.getId(); // 获取插入菜品的ID.
        //向口味表插入数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor ->{
                    dishFlavor.setDishId(dishId);
        });
            dishFlavorMapper.insertBatch(flavors);
            log.info("菜品和口味数据插入成功");
        }

    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断菜品能不能删除是否起售
        for(Long id:ids){
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus()==1){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否关联套餐
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size() > 0){
            //抛出业务异常
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的数据
//        for(Long id:ids){
//            dishMapper.deleteById(id);
//            //删除菜品口味表数据
//            dishFlavorMapper.deleteByDishId(id);
//        }
        //根据菜品id集合批量删除菜品数据
        dishMapper.deleteByIds(ids);

        //根据菜品id集合批量删除菜品口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }
    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        //根据id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //组装VO数据并返回
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品基本信息和口味信息
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改菜品表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        dishMapper.update(dish);
        //删除原有口味
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        //插入新的口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor ->{
                    dishFlavor.setDishId(dishDTO.getId());
        });
        dishFlavorMapper.insertBatch(flavors);
        }

    }
}

