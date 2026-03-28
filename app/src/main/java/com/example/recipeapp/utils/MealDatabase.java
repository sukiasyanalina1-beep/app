package com.example.recipeapp.utils;

import com.example.recipeapp.models.Recipe;
import java.util.Arrays;
import java.util.List;

public class MealDatabase {

    public static List<Recipe> getSeedRecipes() {
        return Arrays.asList(

                createRecipe("Banana Pancakes", "Fluffy and naturally sweet pancakes",
                        "breakfast", Arrays.asList("stovetop"),
                        Arrays.asList("banana", "egg", "flour", "milk", "butter", "sugar", "baking powder"),
                        Arrays.asList(
                                "Mash 2 ripe bananas in a bowl",
                                "Mix in 1 egg, 1 cup flour, 1 cup milk, 1 tbsp sugar and 1 tsp baking powder",
                                "Heat butter in a pan over medium heat",
                                "Pour batter and cook 2 min each side until golden",
                                "Serve with honey or syrup"
                        ), 10, 15, 4),

                createRecipe("Scrambled Eggs", "Creamy and soft scrambled eggs",
                        "breakfast", Arrays.asList("stovetop"),
                        Arrays.asList("egg", "butter", "milk", "salt", "pepper"),
                        Arrays.asList(
                                "Crack 3 eggs into a bowl",
                                "Add 2 tbsp milk, salt and pepper and whisk",
                                "Melt butter in a pan over low heat",
                                "Pour in eggs and stir gently until just set",
                                "Serve immediately on toast"
                        ), 5, 5, 2),

                createRecipe("Avocado Toast", "Simple and healthy breakfast toast",
                        "breakfast", Arrays.asList("stovetop"),
                        Arrays.asList("bread", "avocado", "lemon", "salt", "pepper", "egg"),
                        Arrays.asList(
                                "Toast 2 slices of bread",
                                "Mash 1 avocado with lemon juice, salt and pepper",
                                "Spread avocado on toast",
                                "Top with a fried or poached egg",
                                "Season and serve"
                        ), 5, 10, 2),

                createRecipe("Oatmeal with Fruits", "Warm oats topped with fresh fruit",
                        "breakfast", Arrays.asList("stovetop"),
                        Arrays.asList("oats", "milk", "banana", "apple", "honey", "cinnamon"),
                        Arrays.asList(
                                "Bring 2 cups milk to a simmer",
                                "Add 1 cup oats and stir",
                                "Cook for 5 minutes stirring occasionally",
                                "Top with sliced banana, apple and a drizzle of honey",
                                "Sprinkle with cinnamon and serve"
                        ), 2, 8, 2),

                createRecipe("Smoothie Bowl", "Thick blended smoothie with toppings",
                        "breakfast", Arrays.asList("blender"),
                        Arrays.asList("banana", "yogurt", "milk", "oats", "honey", "almonds"),
                        Arrays.asList(
                                "Blend 2 frozen bananas with 1/2 cup yogurt and 1/4 cup milk",
                                "Pour into a bowl — it should be thick",
                                "Top with oats, sliced almonds and honey",
                                "Serve immediately"
                        ), 5, 5, 1),

                createRecipe("Greek Salad", "Fresh Mediterranean salad",
                        "lunch", Arrays.asList("no equipment"),
                        Arrays.asList("tomato", "cucumber", "bell pepper", "onion", "cheese", "olive oil", "lemon", "oregano"),
                        Arrays.asList(
                                "Chop tomatoes, cucumber, bell pepper and onion into chunks",
                                "Place in a large bowl",
                                "Add crumbled feta cheese",
                                "Drizzle with olive oil and lemon juice",
                                "Season with oregano, salt and pepper and toss"
                        ), 10, 0, 2),

                createRecipe("Chicken Caesar Wrap", "Grilled chicken in a creamy Caesar wrap",
                        "lunch", Arrays.asList("stovetop"),
                        Arrays.asList("chicken", "bread", "lettuce", "cheese", "mayonnaise", "lemon", "garlic", "olive oil"),
                        Arrays.asList(
                                "Season chicken with salt, pepper and garlic",
                                "Grill in olive oil until cooked through, about 6 min each side",
                                "Slice chicken and place on a large wrap",
                                "Add lettuce, parmesan and Caesar dressing",
                                "Roll up tightly and serve"
                        ), 10, 15, 2),

                createRecipe("Tomato Soup", "Creamy homemade tomato soup",
                        "lunch", Arrays.asList("stovetop", "blender"),
                        Arrays.asList("tomato", "onion", "garlic", "butter", "cream", "salt", "pepper", "basil"),
                        Arrays.asList(
                                "Sauté diced onion and garlic in butter until soft",
                                "Add chopped tomatoes and cook for 10 minutes",
                                "Season with salt, pepper and basil",
                                "Blend until smooth",
                                "Stir in cream and heat gently before serving"
                        ), 10, 25, 4),

                createRecipe("Pasta Salad", "Cold pasta with vegetables and dressing",
                        "lunch", Arrays.asList("stovetop"),
                        Arrays.asList("pasta", "tomato", "cucumber", "bell pepper", "olive oil", "lemon", "salt", "pepper", "oregano"),
                        Arrays.asList(
                                "Cook pasta according to package instructions then cool",
                                "Chop tomatoes, cucumber and bell pepper",
                                "Mix pasta with vegetables",
                                "Dress with olive oil, lemon, oregano, salt and pepper",
                                "Refrigerate for 30 minutes before serving"
                        ), 10, 15, 4),

                createRecipe("Lentil Soup", "Hearty and filling lentil soup",
                        "lunch", Arrays.asList("stovetop"),
                        Arrays.asList("lentils", "onion", "garlic", "carrot", "tomato", "cumin", "turmeric", "olive oil", "salt"),
                        Arrays.asList(
                                "Sauté diced onion, garlic and carrot in olive oil",
                                "Add lentils, chopped tomatoes and spices",
                                "Pour in 4 cups water and bring to a boil",
                                "Simmer for 25 minutes until lentils are soft",
                                "Season and serve with bread"
                        ), 10, 30, 4),

                createRecipe("Grilled Chicken", "Simple juicy grilled chicken breast",
                        "dinner", Arrays.asList("stovetop", "oven"),
                        Arrays.asList("chicken", "olive oil", "garlic", "lemon", "rosemary", "salt", "pepper"),
                        Arrays.asList(
                                "Marinate chicken in olive oil, garlic, lemon and rosemary for 30 minutes",
                                "Preheat oven to 200°C",
                                "Sear chicken in an oven-safe pan for 3 min each side",
                                "Transfer to oven and cook for 15 minutes",
                                "Rest for 5 minutes before serving"
                        ), 35, 20, 2),

                createRecipe("Beef Stir Fry", "Quick and flavourful beef stir fry",
                        "dinner", Arrays.asList("stovetop"),
                        Arrays.asList("beef", "bell pepper", "broccoli", "soy sauce", "garlic", "ginger", "olive oil", "rice"),
                        Arrays.asList(
                                "Slice beef thinly and marinate in soy sauce and ginger",
                                "Cook rice according to package instructions",
                                "Heat oil in a wok over high heat",
                                "Stir fry beef for 3 minutes then remove",
                                "Stir fry vegetables for 3 minutes",
                                "Add beef back with soy sauce and garlic",
                                "Serve over rice"
                        ), 15, 20, 3),

                createRecipe("Salmon with Vegetables", "Baked salmon with roasted vegetables",
                        "dinner", Arrays.asList("oven"),
                        Arrays.asList("salmon", "zucchini", "bell pepper", "carrot", "olive oil", "lemon", "garlic", "thyme"),
                        Arrays.asList(
                                "Preheat oven to 200°C",
                                "Chop all vegetables and spread on a baking tray",
                                "Drizzle with olive oil, garlic and thyme",
                                "Place salmon on top and squeeze lemon over",
                                "Bake for 20 minutes until salmon flakes easily"
                        ), 10, 20, 2),

                createRecipe("Spaghetti Bolognese", "Classic Italian meat sauce pasta",
                        "dinner", Arrays.asList("stovetop"),
                        Arrays.asList("pasta", "beef", "tomato", "onion", "garlic", "olive oil", "carrot", "oregano", "basil"),
                        Arrays.asList(
                                "Cook spaghetti according to package instructions",
                                "Sauté onion, garlic and carrot in olive oil",
                                "Add minced beef and brown for 5 minutes",
                                "Add chopped tomatoes, oregano and basil",
                                "Simmer for 20 minutes",
                                "Serve sauce over pasta with parmesan"
                        ), 10, 30, 4),

                createRecipe("Chicken Curry", "Creamy and aromatic chicken curry",
                        "dinner", Arrays.asList("stovetop"),
                        Arrays.asList("chicken", "coconut milk", "onion", "garlic", "ginger", "tomato", "cumin", "turmeric", "chili", "rice"),
                        Arrays.asList(
                                "Sauté onion, garlic and ginger until soft",
                                "Add cumin, turmeric and chili and cook 1 minute",
                                "Add chicken pieces and brown",
                                "Pour in coconut milk and chopped tomatoes",
                                "Simmer for 20 minutes until chicken is cooked",
                                "Serve with rice"
                        ), 15, 30, 4),

                createRecipe("Air Fryer Chicken Wings", "Crispy wings with no deep frying",
                        "dinner", Arrays.asList("air fryer"),
                        Arrays.asList("chicken", "olive oil", "garlic", "paprika", "salt", "pepper", "lemon"),
                        Arrays.asList(
                                "Pat chicken wings dry with paper towel",
                                "Toss with olive oil, garlic, paprika, salt and pepper",
                                "Place in air fryer at 200°C",
                                "Cook for 25 minutes flipping halfway",
                                "Squeeze lemon and serve"
                        ), 10, 25, 3),

                createRecipe("Chocolate Brownies", "Fudgy homemade chocolate brownies",
                        "dessert", Arrays.asList("oven"),
                        Arrays.asList("chocolate", "butter", "sugar", "egg", "flour", "cocoa", "vanilla", "salt"),
                        Arrays.asList(
                                "Preheat oven to 180°C and grease a baking tin",
                                "Melt chocolate and butter together",
                                "Whisk in sugar and eggs",
                                "Fold in flour, cocoa, vanilla and salt",
                                "Pour into tin and bake for 25 minutes",
                                "Cool before cutting into squares"
                        ), 15, 25, 12),

                createRecipe("Banana Ice Cream", "Healthy one ingredient ice cream",
                        "dessert", Arrays.asList("blender"),
                        Arrays.asList("banana", "honey", "vanilla"),
                        Arrays.asList(
                                "Peel and slice bananas then freeze for at least 4 hours",
                                "Blend frozen bananas until smooth and creamy",
                                "Add honey and vanilla and blend again",
                                "Serve immediately or freeze for 1 hour for firmer texture"
                        ), 5, 5, 2),

                createRecipe("Apple Crumble", "Warm apple dessert with crunchy topping",
                        "dessert", Arrays.asList("oven"),
                        Arrays.asList("apple", "flour", "butter", "sugar", "oats", "cinnamon"),
                        Arrays.asList(
                                "Preheat oven to 180°C",
                                "Peel and slice apples and place in a baking dish",
                                "Sprinkle with sugar and cinnamon",
                                "Mix flour, butter, oats and sugar to make crumble",
                                "Spread crumble over apples",
                                "Bake for 30 minutes until golden",
                                "Serve with ice cream or custard"
                        ), 15, 30, 6),

                createRecipe("Pancakes", "Classic fluffy pancakes",
                        "dessert", Arrays.asList("stovetop"),
                        Arrays.asList("flour", "egg", "milk", "butter", "sugar", "baking powder", "vanilla"),
                        Arrays.asList(
                                "Mix flour, sugar and baking powder in a bowl",
                                "Whisk in egg, milk, melted butter and vanilla",
                                "Heat a lightly buttered pan over medium heat",
                                "Pour batter and cook until bubbles form then flip",
                                "Serve with maple syrup and fruit"
                        ), 10, 15, 8),

                createRecipe("Fruit Smoothie", "Fresh blended fruit smoothie",
                        "snack", Arrays.asList("blender"),
                        Arrays.asList("banana", "milk", "yogurt", "honey", "orange", "apple"),
                        Arrays.asList(
                                "Peel and chop all fruit",
                                "Add to blender with milk and yogurt",
                                "Blend until smooth",
                                "Add honey to taste",
                                "Serve immediately over ice"
                        ), 5, 5, 2),

                createRecipe("Hummus with Pita", "Creamy homemade hummus",
                        "snack", Arrays.asList("blender"),
                        Arrays.asList("chickpeas", "lemon", "garlic", "olive oil", "salt", "cumin", "bread"),
                        Arrays.asList(
                                "Drain and rinse chickpeas",
                                "Blend chickpeas with lemon juice, garlic and olive oil",
                                "Add cumin and salt and blend until smooth",
                                "Add water to adjust consistency",
                                "Serve with pita bread and a drizzle of olive oil"
                        ), 10, 0, 4),

                createRecipe("Air Fryer Fries", "Crispy homemade fries in the air fryer",
                        "snack", Arrays.asList("air fryer"),
                        Arrays.asList("potato", "olive oil", "salt", "pepper", "paprika"),
                        Arrays.asList(
                                "Peel and cut potatoes into fries",
                                "Soak in cold water for 30 minutes then dry",
                                "Toss with olive oil, salt, pepper and paprika",
                                "Air fry at 200°C for 20 minutes shaking halfway",
                                "Serve immediately"
                        ), 35, 20, 3)
        );
    }

    private static Recipe createRecipe(String title, String description, String mealType,
                                       List<String> equipment, List<String> ingredients,
                                       List<String> steps, int prepTime, int cookTime, int servings) {
        Recipe r = new Recipe();
        r.setTitle(title);
        r.setDescription(description);
        r.setMealType(mealType);
        r.setEquipment(equipment);
        r.setIngredients(ingredients);
        r.setSteps(steps);
        r.setPrepTimeMinutes(prepTime);
        r.setCookTimeMinutes(cookTime);
        r.setServings(servings);
        r.setUserCreated(false);
        r.setAuthorName("RecipeApp");
        r.setCreatedAt(System.currentTimeMillis());
        return r;
    }
}